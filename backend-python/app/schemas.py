from pydantic import BaseModel, Field


class LoginRequest(BaseModel):
    username: str
    password: str


class RegisterRequest(BaseModel):
    username: str
    password: str
    realName: str
    college: str
    email: str | None = None


class LoginResponse(BaseModel):
    token: str
    userId: int
    username: str
    role: str


class ChatSendRequest(BaseModel):
    sessionId: int | None = None
    message: str = Field(min_length=1)


class RagReference(BaseModel):
    documentName: str
    content: str
    score: float | None = None
    denseScore: float | None = None
    keywordScore: float | None = None
    rrfScore: float | None = None
    rerankScore: float | None = None


class ClassificationResult(BaseModel):
    intent: str = "CHAT"
    riskLevel: str = "LOW"
    riskType: str = "none"
    reason: str = ""


class ChatSendResponse(BaseModel):
    sessionId: int
    reply: str
    intent: str
    riskLevel: str
    riskType: str
    ragHit: bool
    references: list[RagReference] = []
    actions: list[str] = []


class EmailTestRequest(BaseModel):
    receiver: str | None = None
    subject: str | None = None
    content: str | None = None


def ok(data=None, message: str = "OK") -> dict:
    return {"success": True, "message": message, "data": data}


def fail(message: str) -> dict:
    return {"success": False, "message": message, "data": None}
