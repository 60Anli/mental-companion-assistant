from collections.abc import Generator

from sqlalchemy import create_engine, text
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.config import get_settings


class Base(DeclarativeBase):
    pass


settings = get_settings()
engine = create_engine(settings.mysql_url, pool_pre_ping=True, pool_recycle=3600)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, expire_on_commit=False)


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_schema() -> None:
    """Create missing tables for the Python backend when MySQL was not initialized by Docker."""
    statements = [
        """
        CREATE TABLE IF NOT EXISTS sys_user (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            username VARCHAR(64) NOT NULL UNIQUE,
            password VARCHAR(255) NOT NULL,
            real_name VARCHAR(64),
            college VARCHAR(128),
            department VARCHAR(128),
            email VARCHAR(128),
            role VARCHAR(32) NOT NULL,
            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS chat_session (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            user_id BIGINT NOT NULL,
            title VARCHAR(128) NOT NULL,
            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_chat_session_user (user_id)
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS chat_message (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            session_id BIGINT NOT NULL,
            role VARCHAR(32) NOT NULL,
            content TEXT NOT NULL,
            intent VARCHAR(32),
            risk_level VARCHAR(32),
            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_chat_message_session (session_id)
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS workflow_record (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            user_id BIGINT NOT NULL,
            session_id BIGINT NOT NULL,
            user_message TEXT NOT NULL,
            intent VARCHAR(32) NOT NULL,
            risk_type VARCHAR(64),
            risk_level VARCHAR(32) NOT NULL,
            rag_hit TINYINT(1) NOT NULL DEFAULT 0,
            rag_references MEDIUMTEXT,
            ai_reply MEDIUMTEXT NOT NULL,
            excel_exported TINYINT(1) NOT NULL DEFAULT 0,
            email_sent TINYINT(1) NOT NULL DEFAULT 0,
            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_workflow_user_time (user_id, create_time),
            INDEX idx_workflow_intent (intent),
            INDEX idx_workflow_risk (risk_level)
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS risk_record (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            user_id BIGINT NOT NULL,
            session_id BIGINT NOT NULL,
            user_message TEXT NOT NULL,
            risk_type VARCHAR(64),
            risk_level VARCHAR(32) NOT NULL,
            ai_reply MEDIUMTEXT NOT NULL,
            handled TINYINT(1) NOT NULL DEFAULT 0,
            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_risk_user_time (user_id, create_time),
            INDEX idx_risk_handled (handled)
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS knowledge_document (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            file_name VARCHAR(255) NOT NULL,
            content MEDIUMTEXT NOT NULL,
            chunk_count INT NOT NULL DEFAULT 0,
            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS knowledge_chunk (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            document_id BIGINT NOT NULL,
            document_name VARCHAR(255) NOT NULL,
            chunk_index INT NOT NULL,
            content MEDIUMTEXT NOT NULL,
            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_knowledge_chunk_doc (document_id),
            INDEX idx_knowledge_chunk_time (create_time)
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS email_alert_log (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            risk_record_id BIGINT,
            receiver VARCHAR(255) NOT NULL,
            subject VARCHAR(255) NOT NULL,
            content MEDIUMTEXT NOT NULL,
            send_status VARCHAR(32) NOT NULL,
            error_message TEXT,
            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_email_risk (risk_record_id),
            INDEX idx_email_time (create_time)
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS excel_export_log (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            record_id BIGINT,
            file_path VARCHAR(500) NOT NULL,
            export_status VARCHAR(32) NOT NULL,
            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_excel_record (record_id)
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS user_memory (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            user_id BIGINT NOT NULL,
            memory_key VARCHAR(128) NOT NULL,
            memory_type VARCHAR(64) NOT NULL,
            content VARCHAR(1000) NOT NULL,
            source_session_id BIGINT,
            importance INT NOT NULL DEFAULT 3,
            last_used_time DATETIME,
            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE KEY uk_user_memory_key (user_id, memory_key),
            INDEX idx_user_memory_user_time (user_id, update_time),
            INDEX idx_user_memory_type (memory_type)
        )
        """,
    ]
    with engine.begin() as conn:
        for statement in statements:
            conn.execute(text(statement))
