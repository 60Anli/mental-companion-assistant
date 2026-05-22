package com.example.mentalcompanion.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mentalcompanion.config.RetrievalProperties;
import com.example.mentalcompanion.domain.entity.KnowledgeChunk;
import com.example.mentalcompanion.mapper.KnowledgeChunkMapper;
import jakarta.annotation.PostConstruct;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@DependsOn("knowledgeSchemaInitializer")
public class Bm25KnowledgeSearchService {

    private static final String FIELD_CHUNK_ID = "chunkId";
    private static final String FIELD_DOCUMENT_NAME = "documentName";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_CONTENT_TEXT = "contentText";

    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final RetrievalProperties retrievalProperties;

    public Bm25KnowledgeSearchService(
            KnowledgeChunkMapper knowledgeChunkMapper,
            RetrievalProperties retrievalProperties
    ) {
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.retrievalProperties = retrievalProperties;
    }

    @PostConstruct
    public void rebuildIndexOnStartup() {
        if (retrievalProperties.rebuildOnStartup()) {
            rebuildIndex();
        }
    }

    public synchronized void rebuildIndex() {
        List<KnowledgeChunk> chunks = knowledgeChunkMapper.selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                .orderByAsc(KnowledgeChunk::getId));
        writeChunks(chunks, IndexWriterConfig.OpenMode.CREATE);
    }

    public synchronized void indexChunks(List<KnowledgeChunk> chunks) {
        writeChunks(chunks, IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    }

    public List<Bm25SearchResult> search(String queryText, int topK) {
        if (queryText == null || queryText.isBlank()) {
            return List.of();
        }
        try {
            Path indexPath = indexPath();
            Files.createDirectories(indexPath);
            try (FSDirectory directory = FSDirectory.open(indexPath)) {
                if (!DirectoryReader.indexExists(directory)) {
                    return List.of();
                }
                try (Analyzer analyzer = new SmartChineseAnalyzer();
                     DirectoryReader reader = DirectoryReader.open(directory)) {
                    IndexSearcher searcher = new IndexSearcher(reader);
                    searcher.setSimilarity(new BM25Similarity());
                    QueryParser parser = new QueryParser(FIELD_CONTENT_TEXT, analyzer);
                    Query query = parser.parse(QueryParser.escape(queryText));
                    TopDocs topDocs = searcher.search(query, Math.max(1, topK));
                    List<Bm25SearchResult> results = new ArrayList<>();
                    ScoreDoc[] scoreDocs = topDocs.scoreDocs;
                    for (int i = 0; i < scoreDocs.length; i++) {
                        Document document = searcher.doc(scoreDocs[i].doc);
                        results.add(new Bm25SearchResult(
                                document.get(FIELD_DOCUMENT_NAME),
                                document.get(FIELD_CONTENT),
                                i + 1,
                                scoreDocs[i].score
                        ));
                    }
                    return results;
                }
            }
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void writeChunks(List<KnowledgeChunk> chunks, IndexWriterConfig.OpenMode openMode) {
        try {
            Path indexPath = indexPath();
            Files.createDirectories(indexPath);
            try (Analyzer analyzer = new SmartChineseAnalyzer();
                 FSDirectory directory = FSDirectory.open(indexPath)) {
                IndexWriterConfig config = new IndexWriterConfig(analyzer);
                config.setOpenMode(openMode);
                config.setSimilarity(new BM25Similarity());
                try (IndexWriter writer = new IndexWriter(directory, config)) {
                    for (KnowledgeChunk chunk : chunks) {
                        if (chunk.getId() == null || chunk.getContent() == null || chunk.getContent().isBlank()) {
                            continue;
                        }
                        writer.updateDocument(
                                new Term(FIELD_CHUNK_ID, String.valueOf(chunk.getId())),
                                toLuceneDocument(chunk)
                        );
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("BM25 index write failed: " + ex.getMessage(), ex);
        }
    }

    private Document toLuceneDocument(KnowledgeChunk chunk) {
        Document document = new Document();
        document.add(new StringField(FIELD_CHUNK_ID, String.valueOf(chunk.getId()), Field.Store.YES));
        document.add(new StringField(FIELD_DOCUMENT_NAME, chunk.getDocumentName(), Field.Store.YES));
        document.add(new StoredField(FIELD_CONTENT, chunk.getContent()));
        document.add(new TextField(FIELD_CONTENT_TEXT, chunk.getContent(), Field.Store.NO));
        return document;
    }

    private Path indexPath() {
        return Path.of(retrievalProperties.luceneIndexPath()).toAbsolutePath().normalize();
    }
}
