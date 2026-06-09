from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=(".env", "../.env", "backend-python/.env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "mental-companion-assistant-python"
    server_host: str = "0.0.0.0"
    server_port: int = 8080

    mysql_url: str = "mysql+pymysql://root:1111@127.0.0.1:3307/mental_companion?charset=utf8mb4"
    redis_url: str = "redis://127.0.0.1:6379/0"

    jwt_secret: str = "mental-companion-assistant-demo-secret-please-change-2026"
    jwt_expire_minutes: int = 720

    llm_provider: str = "openai"
    llm_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode"
    llm_api_key: str = ""
    llm_model: str = "qwen-plus"
    llm_vision_model: str = "qwen-vl-plus"
    llm_embedding_model: str = "text-embedding-v3"

    max_image_size_mb: int = 10
    max_media_count: int = 5
    media_upload_dir: str = "./data/uploads"
    media_allowed_image_types: str = "image/jpeg,image/png,image/gif,image/webp"

    qdrant_url: str = "http://127.0.0.1:6333"
    qdrant_collection: str = "mental_companion_knowledge"
    qdrant_vector_size: int = 1024

    rag_final_top_k: int = 3
    rag_dense_top_k: int = 12
    rag_keyword_top_k: int = 12
    rag_rrf_k: int = 60
    reranker_enabled: bool = True
    cross_encoder_model: str = "BAAI/bge-reranker-base"

    excel_workflow_path: str = "./data/workflow-records.xlsx"
    excel_export_dir: str = "./data/exported"

    mail_enabled: bool = False
    mail_host: str = "smtp.qq.com"
    mail_port: int = 465
    mail_username: str = ""
    mail_password: str = ""
    mail_from: str = ""
    mail_alert_receiver: str = "admin@example.com"
    mail_use_ssl: bool = True
    mail_use_tls: bool = False
    mail_teacher_name: str = "李老师"
    mail_teacher_department: str = "学生心理健康中心"

    fine_tune_enabled: bool = False
    fine_tune_base_model: str = "Qwen2.5"
    fine_tune_adapter_type: str = "QLoRA"
    fine_tune_adapter_path: str = "./models/adapters/qwen2.5-mental"
    fine_tune_ollama_model: str = "qwen2.5-mental:latest"
    fine_tune_training_profile: str = "emotion-dialog-sft"

    def ensure_dirs(self) -> None:
        Path(self.excel_workflow_path).parent.mkdir(parents=True, exist_ok=True)
        Path(self.excel_export_dir).mkdir(parents=True, exist_ok=True)
        Path(self.media_upload_dir).mkdir(parents=True, exist_ok=True)


@lru_cache
def get_settings() -> Settings:
    settings = Settings()
    settings.ensure_dirs()
    return settings
