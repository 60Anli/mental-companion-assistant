from __future__ import annotations

import json
from typing import Any

import httpx

from app.config import get_settings


# Type alias for multimodal content — either a plain string or an array of
# {"type": "text"/"image_url", ...} blocks (OpenAI / DashScope vision format).
MultimodalContent = str | list[dict[str, object]]


class LlmClient:
    def __init__(self) -> None:
        self.settings = get_settings()
        self.timeout = httpx.Timeout(120.0, connect=10.0)

    # ── public helpers ──────────────────────────────────────────────

    def chat(
        self,
        system_prompt: str,
        user_content: MultimodalContent,
        *,
        force_vision: bool = False,
    ) -> str:
        """Send a chat request. *user_content* may be a string (text-only) or a
        multimodal list of ``{"type":"text"/"image_url",...}`` blocks."""
        if self.settings.llm_provider.lower() == "ollama":
            return self._ollama_chat(system_prompt, user_content)
        return self._openai_chat(system_prompt, user_content, force_vision=force_vision)

    def chat_json(
        self,
        system_prompt: str,
        user_content: MultimodalContent,
        *,
        force_vision: bool = False,
    ) -> dict[str, Any]:
        text = self.chat(system_prompt, user_content, force_vision=force_vision)
        start = text.find("{")
        end = text.rfind("}")
        if start >= 0 and end > start:
            text = text[start : end + 1]
        return json.loads(text)

    def embedding(self, text: str) -> list[float]:
        if self.settings.llm_provider.lower() == "ollama":
            return self._ollama_embedding(text)
        return self._openai_embedding(text)

    # ── OpenAI-compatible (DashScope) ───────────────────────────────

    def _openai_chat(
        self,
        system_prompt: str,
        user_content: MultimodalContent,
        *,
        force_vision: bool = False,
    ) -> str:
        url = self._join_url("/v1/chat/completions")
        headers = {"Authorization": f"Bearer {self.settings.llm_api_key}"}

        # Pick the right model — vision model when images are present
        model = self.settings.llm_model
        if force_vision or (isinstance(user_content, list) and len(user_content) > 1):
            model = self.settings.llm_vision_model

        body: dict[str, Any] = {
            "model": model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_content},
            ],
            "temperature": 0.4,
        }
        with httpx.Client(timeout=self.timeout) as client:
            response = client.post(url, json=body, headers=headers)
            response.raise_for_status()
            return response.json()["choices"][0]["message"]["content"]

    def _openai_embedding(self, text: str) -> list[float]:
        url = self._join_url("/v1/embeddings")
        headers = {"Authorization": f"Bearer {self.settings.llm_api_key}"}
        body = {"model": self.settings.llm_embedding_model, "input": text}
        with httpx.Client(timeout=self.timeout) as client:
            response = client.post(url, json=body, headers=headers)
            response.raise_for_status()
            return response.json()["data"][0]["embedding"]

    # ── Ollama ──────────────────────────────────────────────────────

    def _ollama_chat(
        self,
        system_prompt: str,
        user_content: MultimodalContent,
    ) -> str:
        # Ollama's /api/chat also supports the multimodal content array
        # format when the underlying model understands images.
        url = self.settings.llm_base_url.rstrip("/") + "/api/chat"
        body = {
            "model": self.settings.llm_model,
            "stream": False,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_content},
            ],
        }
        with httpx.Client(timeout=self.timeout) as client:
            response = client.post(url, json=body)
            response.raise_for_status()
            return response.json()["message"]["content"]

    def _ollama_embedding(self, text: str) -> list[float]:
        url = self.settings.llm_base_url.rstrip("/") + "/api/embeddings"
        body = {"model": self.settings.llm_embedding_model, "prompt": text}
        with httpx.Client(timeout=self.timeout) as client:
            response = client.post(url, json=body)
            response.raise_for_status()
            return response.json()["embedding"]

    # ── helpers ─────────────────────────────────────────────────────

    def _join_url(self, path: str) -> str:
        base = self.settings.llm_base_url.rstrip("/")
        if base.endswith("/v1") and path.startswith("/v1/"):
            return base + path.removeprefix("/v1")
        return base + path
