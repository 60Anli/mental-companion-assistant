import json
from typing import Any

import httpx

from app.config import get_settings


class LlmClient:
    def __init__(self) -> None:
        self.settings = get_settings()
        self.timeout = httpx.Timeout(90.0, connect=10.0)

    def chat(self, system_prompt: str, user_prompt: str) -> str:
        if self.settings.llm_provider.lower() == "ollama":
            return self._ollama_chat(system_prompt, user_prompt)
        return self._openai_chat(system_prompt, user_prompt)

    def chat_json(self, system_prompt: str, user_prompt: str) -> dict[str, Any]:
        text = self.chat(system_prompt, user_prompt)
        start = text.find("{")
        end = text.rfind("}")
        if start >= 0 and end > start:
            text = text[start : end + 1]
        return json.loads(text)

    def embedding(self, text: str) -> list[float]:
        if self.settings.llm_provider.lower() == "ollama":
            return self._ollama_embedding(text)
        return self._openai_embedding(text)

    def _openai_chat(self, system_prompt: str, user_prompt: str) -> str:
        url = self._join_url("/v1/chat/completions")
        headers = {"Authorization": f"Bearer {self.settings.llm_api_key}"}
        body = {
            "model": self.settings.llm_model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
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

    def _ollama_chat(self, system_prompt: str, user_prompt: str) -> str:
        url = self.settings.llm_base_url.rstrip("/") + "/api/chat"
        body = {
            "model": self.settings.llm_model,
            "stream": False,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
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

    def _join_url(self, path: str) -> str:
        base = self.settings.llm_base_url.rstrip("/")
        if base.endswith("/v1") and path.startswith("/v1/"):
            return base + path.removeprefix("/v1")
        return base + path
