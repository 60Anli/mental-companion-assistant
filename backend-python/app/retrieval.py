from __future__ import annotations

import hashlib
import logging
from dataclasses import dataclass
from typing import Iterable

import jieba
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, PointStruct, VectorParams
from rank_bm25 import BM25Okapi
from sqlalchemy.orm import Session

from app.config import get_settings
from app.llm import LlmClient
from app.models import KnowledgeChunk
from app.schemas import RagReference

logger = logging.getLogger(__name__)


@dataclass
class RetrievalCandidate:
    chunk_id: int
    document_id: int | None
    document_name: str
    content: str
    dense_rank: int | None = None
    keyword_rank: int | None = None
    dense_score: float | None = None
    keyword_score: float | None = None
    rrf_score: float = 0.0
    rerank_score: float | None = None


def chunk_text(text: str, chunk_size: int = 520, overlap: int = 80) -> list[str]:
    clean = "\n".join(line.strip() for line in text.splitlines() if line.strip())
    if len(clean) <= chunk_size:
        return [clean] if clean else []
    chunks = []
    start = 0
    while start < len(clean):
        end = min(start + chunk_size, len(clean))
        chunks.append(clean[start:end])
        if end == len(clean):
            break
        start = max(0, end - overlap)
    return chunks


def tokenize(text: str) -> list[str]:
    return [token.strip().lower() for token in jieba.lcut(text or "") if token.strip()]


class QdrantVectorStore:
    def __init__(self, llm_client: LlmClient) -> None:
        self.settings = get_settings()
        self.llm_client = llm_client
        self.client = QdrantClient(url=self.settings.qdrant_url)
        self.ensure_collection()

    def ensure_collection(self) -> None:
        try:
            self.client.get_collection(self.settings.qdrant_collection)
        except Exception:
            try:
                self.client.create_collection(
                    collection_name=self.settings.qdrant_collection,
                    vectors_config=VectorParams(size=self.settings.qdrant_vector_size, distance=Distance.COSINE),
                )
            except Exception as exc:
                logger.warning("Qdrant collection is not ready yet: %s", exc)

    def upsert_chunks(self, chunks: Iterable[KnowledgeChunk]) -> None:
        points = []
        for chunk in chunks:
            vector = self.llm_client.embedding(chunk.content)
            points.append(
                PointStruct(
                    id=int(chunk.id),
                    vector=vector,
                    payload={
                        "chunk_id": chunk.id,
                        "document_id": chunk.document_id,
                        "document_name": chunk.document_name,
                        "content": chunk.content,
                    },
                )
            )
        if points:
            self.client.upsert(collection_name=self.settings.qdrant_collection, points=points)

    def search(self, query: str, top_k: int) -> list[RetrievalCandidate]:
        vector = self.llm_client.embedding(query)
        try:
            hits = self.client.search(
                collection_name=self.settings.qdrant_collection,
                query_vector=vector,
                limit=top_k,
                with_payload=True,
            )
        except AttributeError:
            response = self.client.query_points(
                collection_name=self.settings.qdrant_collection,
                query=vector,
                limit=top_k,
                with_payload=True,
            )
            hits = response.points
        candidates = []
        for rank, hit in enumerate(hits, start=1):
            payload = hit.payload or {}
            candidates.append(
                RetrievalCandidate(
                    chunk_id=int(payload.get("chunk_id") or hit.id),
                    document_id=payload.get("document_id"),
                    document_name=str(payload.get("document_name") or "unknown"),
                    content=str(payload.get("content") or ""),
                    dense_rank=rank,
                    dense_score=float(hit.score) if hit.score is not None else None,
                )
            )
        return candidates


class Bm25KeywordRetriever:
    def search(self, db: Session, query: str, top_k: int) -> list[RetrievalCandidate]:
        chunks = db.query(KnowledgeChunk).order_by(KnowledgeChunk.id.desc()).limit(3000).all()
        if not chunks:
            return []
        corpus = [tokenize(chunk.content) for chunk in chunks]
        bm25 = BM25Okapi(corpus)
        scores = bm25.get_scores(tokenize(query))
        ranked = sorted(enumerate(scores), key=lambda item: item[1], reverse=True)[:top_k]
        candidates = []
        for rank, (idx, score) in enumerate(ranked, start=1):
            if score <= 0:
                continue
            chunk = chunks[idx]
            candidates.append(
                RetrievalCandidate(
                    chunk_id=chunk.id,
                    document_id=chunk.document_id,
                    document_name=chunk.document_name,
                    content=chunk.content,
                    keyword_rank=rank,
                    keyword_score=float(score),
                )
            )
        return candidates


class CrossEncoderReranker:
    def __init__(self) -> None:
        self.settings = get_settings()
        self._model = None
        self._load_error: str | None = None

    def rerank(self, query: str, candidates: list[RetrievalCandidate], top_k: int) -> list[RetrievalCandidate]:
        if not candidates:
            return []
        if not self.settings.reranker_enabled:
            return sorted(candidates, key=lambda item: item.rrf_score, reverse=True)[:top_k]
        model = self._get_model()
        if model is None:
            return sorted(candidates, key=lambda item: item.rrf_score, reverse=True)[:top_k]
        pairs = [(query, candidate.content) for candidate in candidates]
        scores = model.predict(pairs)
        for candidate, score in zip(candidates, scores, strict=False):
            candidate.rerank_score = float(score)
        return sorted(candidates, key=lambda item: item.rerank_score or 0.0, reverse=True)[:top_k]

    def _get_model(self):
        if self._model is not None or self._load_error is not None:
            return self._model
        try:
            from sentence_transformers import CrossEncoder

            self._model = CrossEncoder(self.settings.cross_encoder_model)
        except Exception as exc:
            self._load_error = str(exc)
            logger.warning("Cross-Encoder reranker disabled: %s", exc)
        return self._model


class HybridRetrievalPipeline:
    def __init__(self, vector_store: QdrantVectorStore) -> None:
        self.settings = get_settings()
        self.vector_store = vector_store
        self.keyword_retriever = Bm25KeywordRetriever()
        self.reranker = CrossEncoderReranker()

    def search(self, db: Session, query: str) -> list[RagReference]:
        dense = self._safe_dense_search(query)
        keyword = self.keyword_retriever.search(db, query, self.settings.rag_keyword_top_k)
        fused = self._rrf_fusion(dense, keyword)
        reranked = self.reranker.rerank(query, fused, self.settings.rag_final_top_k)
        return [
            RagReference(
                documentName=item.document_name,
                content=item.content,
                score=item.rerank_score if item.rerank_score is not None else item.rrf_score,
                denseScore=item.dense_score,
                keywordScore=item.keyword_score,
                rrfScore=item.rrf_score,
                rerankScore=item.rerank_score,
            )
            for item in reranked
        ]

    def _safe_dense_search(self, query: str) -> list[RetrievalCandidate]:
        try:
            return self.vector_store.search(query, self.settings.rag_dense_top_k)
        except Exception as exc:
            logger.warning("Qdrant dense retrieval failed, continue with BM25 only: %s", exc)
            return []

    def _rrf_fusion(
        self,
        dense: list[RetrievalCandidate],
        keyword: list[RetrievalCandidate],
    ) -> list[RetrievalCandidate]:
        merged: dict[int, RetrievalCandidate] = {}
        for item in dense:
            merged[item.chunk_id] = item
            if item.dense_rank is not None:
                item.rrf_score += 1.0 / (self.settings.rag_rrf_k + item.dense_rank)
        for item in keyword:
            existing = merged.get(item.chunk_id)
            if existing is None:
                existing = item
                merged[item.chunk_id] = existing
            else:
                existing.keyword_rank = item.keyword_rank
                existing.keyword_score = item.keyword_score
                existing.content = existing.content or item.content
                existing.document_name = existing.document_name or item.document_name
            if item.keyword_rank is not None:
                existing.rrf_score += 1.0 / (self.settings.rag_rrf_k + item.keyword_rank)
        return sorted(merged.values(), key=lambda candidate: candidate.rrf_score, reverse=True)


def stable_memory_key(text: str) -> str:
    return hashlib.sha1(text.encode("utf-8")).hexdigest()[:24]
