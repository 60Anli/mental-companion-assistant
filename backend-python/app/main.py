import logging
from contextlib import asynccontextmanager
from functools import lru_cache
from typing import AsyncGenerator

from fastapi import Depends, FastAPI, File, HTTPException, Request, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from sqlalchemy.orm import Session

from app.config import get_settings
from app.database import SessionLocal, get_db, init_schema
from app.llm import LlmClient
from app.agent import LangGraphReActAgent
from app.models import (
    ChatMessage,
    ChatSession,
    EmailAlertLog,
    KnowledgeDocument,
    RiskRecord,
    SysUser,
    UserMemory,
    WorkflowRecord,
)
from app.retrieval import HybridRetrievalPipeline, QdrantVectorStore
from app.schemas import ChatSendRequest, EmailTestRequest, LoginRequest, LoginResponse, RegisterRequest, ok
from app.security import admin_user, create_token, current_user, hash_password, verify_password
from app.services import EmailService, ExcelService, KnowledgeService, seed_default_users, to_api_list
from app.utils import model_to_dict

logger = logging.getLogger(__name__)

settings = get_settings()


@asynccontextmanager
async def lifespan(_app: FastAPI) -> AsyncGenerator[None, None]:
    logger.info("Starting Mental Companion Python Backend...")
    try:
        if init_schema():
            db = SessionLocal()
            try:
                seed_default_users(db)
                logger.info("Database initialized and default users seeded.")
            finally:
                db.close()
        else:
            logger.warning("Database schema initialization skipped (MySQL may not be available).")
    except Exception as exc:
        logger.warning("Database initialization failed (services may not be ready): %s", exc)
    yield
    logger.info("Shutting down...")


app = FastAPI(title=settings.app_name, lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(HTTPException)
async def http_exception_handler(_: Request, exc: HTTPException):
    return JSONResponse(status_code=exc.status_code, content={"success": False, "message": str(exc.detail), "data": None})


@app.exception_handler(Exception)
async def global_exception_handler(_: Request, exc: Exception):
    logger.exception("Unhandled exception")
    return JSONResponse(status_code=500, content={"success": False, "message": str(exc), "data": None})


@lru_cache
def llm_client() -> LlmClient:
    return LlmClient()


@lru_cache
def vector_store() -> QdrantVectorStore:
    return QdrantVectorStore(llm_client())


@lru_cache
def retrieval_pipeline() -> HybridRetrievalPipeline:
    return HybridRetrievalPipeline(vector_store())


@lru_cache
def chat_service() -> LangGraphReActAgent:
    return LangGraphReActAgent(retrieval_pipeline(), llm_client())


@lru_cache
def knowledge_service() -> KnowledgeService:
    return KnowledgeService(vector_store())


@lru_cache
def email_service() -> EmailService:
    return EmailService()


@lru_cache
def excel_service() -> ExcelService:
    return ExcelService()


@app.get("/api/health")
def health():
    return ok({"status": "UP", "backend": "python", "vectorStore": "qdrant"})


@app.post("/api/auth/login")
def login(request: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(SysUser).filter(SysUser.username == request.username).first()
    if not user or not verify_password(request.password, user.password):
        raise HTTPException(status_code=400, detail="用户名或密码错误")
    token = create_token(user)
    return ok(LoginResponse(token=token, userId=user.id, username=user.username, role=user.role).model_dump())


@app.post("/api/auth/register")
def register(request: RegisterRequest, db: Session = Depends(get_db)):
    username = request.username.strip()
    if db.query(SysUser).filter(SysUser.username == username).first():
        raise HTTPException(status_code=400, detail="用户名已存在")
    user = SysUser(
        username=username,
        password=hash_password(request.password),
        real_name=request.realName.strip(),
        college=request.college.strip(),
        email=request.email.strip() if request.email else None,
        role="USER",
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    token = create_token(user)
    return ok(LoginResponse(token=token, userId=user.id, username=user.username, role=user.role).model_dump(), "注册成功")


@app.post("/api/chat/send")
def send_chat(
    request: ChatSendRequest,
    user: SysUser = Depends(current_user),
    db: Session = Depends(get_db),
):
    data = chat_service().process_message(db, user.id, request.sessionId, request.message)
    return ok(data)


@app.get("/api/chat/sessions")
def chat_sessions(user: SysUser = Depends(current_user), db: Session = Depends(get_db)):
    sessions = (
        db.query(ChatSession)
        .filter(ChatSession.user_id == user.id)
        .order_by(ChatSession.create_time.desc())
        .all()
    )
    return ok(to_api_list(sessions))


@app.get("/api/chat/sessions/{session_id}/messages")
def chat_messages(session_id: int, user: SysUser = Depends(current_user), db: Session = Depends(get_db)):
    session = db.get(ChatSession, session_id)
    if not session or session.user_id != user.id:
        raise HTTPException(status_code=404, detail="会话不存在")
    messages = (
        db.query(ChatMessage)
        .filter(ChatMessage.session_id == session_id)
        .order_by(ChatMessage.create_time.asc())
        .all()
    )
    return ok(to_api_list(messages))


@app.delete("/api/chat/sessions/{session_id}")
def delete_session(session_id: int, user: SysUser = Depends(current_user), db: Session = Depends(get_db)):
    session = db.get(ChatSession, session_id)
    if not session or session.user_id != user.id:
        raise HTTPException(status_code=404, detail="会话不存在")
    db.query(ChatMessage).filter(ChatMessage.session_id == session_id).delete()
    db.delete(session)
    db.commit()
    return ok(None, "会话已删除")


@app.post("/api/admin/knowledge/upload")
def upload_knowledge(
    file: UploadFile = File(...),
    _: SysUser = Depends(admin_user),
    db: Session = Depends(get_db),
):
    name = file.filename or "knowledge.txt"
    if not (name.endswith(".txt") or name.endswith(".md")):
        raise HTTPException(status_code=400, detail="只支持 txt / md 文档")
    content = file.file.read().decode("utf-8")
    document = knowledge_service().upload(db, name, content)
    return ok(model_to_dict(document))


@app.get("/api/admin/knowledge/list")
def knowledge_list(_: SysUser = Depends(admin_user), db: Session = Depends(get_db)):
    documents = db.query(KnowledgeDocument).order_by(KnowledgeDocument.create_time.desc()).all()
    return ok(to_api_list(documents))


@app.get("/api/admin/workflow-records")
def workflow_records(_: SysUser = Depends(admin_user), db: Session = Depends(get_db)):
    records = db.query(WorkflowRecord).order_by(WorkflowRecord.create_time.desc()).all()
    return ok(to_api_list(records))


@app.get("/api/admin/workflow-records/export")
def export_workflow_records(_: SysUser = Depends(admin_user), db: Session = Depends(get_db)):
    records = db.query(WorkflowRecord).order_by(WorkflowRecord.create_time.desc()).all()
    path = excel_service().export_all(db, records)
    return FileResponse(path, filename=path.name, media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")


@app.get("/api/admin/risk-records")
def risk_records(_: SysUser = Depends(admin_user), db: Session = Depends(get_db)):
    records = db.query(RiskRecord).order_by(RiskRecord.create_time.desc()).all()
    return ok(to_api_list(records))


@app.get("/api/admin/memories")
def memories(_: SysUser = Depends(admin_user), db: Session = Depends(get_db)):
    records = db.query(UserMemory).order_by(UserMemory.update_time.desc()).all()
    return ok(to_api_list(records))


@app.get("/api/admin/model/runtime")
def model_runtime(_: SysUser = Depends(admin_user)):
    return ok(
        {
            "provider": settings.llm_provider,
            "chatModel": settings.llm_model,
            "embeddingModel": settings.llm_embedding_model,
            "fineTuneEnabled": settings.fine_tune_enabled,
            "fineTuneBaseModel": settings.fine_tune_base_model,
            "adapterType": settings.fine_tune_adapter_type,
            "adapterPath": settings.fine_tune_adapter_path,
            "ollamaModel": settings.fine_tune_ollama_model,
            "trainingProfile": settings.fine_tune_training_profile,
            "agentFramework": "LangGraph ReAct StateGraph",
            "retrievalPipeline": "Qdrant dense + BM25 keyword + RRF fusion + Cross-Encoder rerank",
            "rerankerModel": settings.cross_encoder_model,
        }
    )


@app.get("/api/admin/users")
def users(_: SysUser = Depends(admin_user), db: Session = Depends(get_db)):
    records = db.query(SysUser).order_by(SysUser.create_time.desc()).all()
    return ok(
        [
            {
                "id": item.id,
                "username": item.username,
                "realName": item.real_name,
                "college": item.college,
                "department": item.department,
                "email": item.email,
                "role": item.role,
                "createTime": item.create_time.isoformat(sep=" ") if item.create_time else None,
            }
            for item in records
        ]
    )


@app.post("/api/admin/email/test")
def test_email(
    request: EmailTestRequest | None = None,
    _: SysUser = Depends(admin_user),
    db: Session = Depends(get_db),
):
    data = request or EmailTestRequest()
    sent = email_service().send_test(db, data.receiver, data.subject, data.content)
    db.commit()
    return ok(sent)


@app.get("/api/admin/email/logs")
def email_logs(_: SysUser = Depends(admin_user), db: Session = Depends(get_db)):
    logs = db.query(EmailAlertLog).order_by(EmailAlertLog.create_time.desc()).all()
    return ok(to_api_list(logs))
