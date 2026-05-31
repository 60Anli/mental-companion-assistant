from datetime import datetime, timedelta, timezone
from typing import Annotated

import jwt
from fastapi import Depends, Header, HTTPException
from passlib.context import CryptContext
from sqlalchemy.orm import Session

from app.config import get_settings
from app.database import get_db
from app.models import SysUser

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(raw_password: str, stored_password: str) -> bool:
    if stored_password.startswith("{noop}"):
        return raw_password == stored_password.removeprefix("{noop}")
    if stored_password == raw_password:
        return True
    try:
        return pwd_context.verify(raw_password, stored_password)
    except Exception:
        return False


def create_token(user: SysUser) -> str:
    settings = get_settings()
    now = datetime.now(timezone.utc)
    payload = {
        "sub": str(user.id),
        "username": user.username,
        "role": user.role,
        "iat": int(now.timestamp()),
        "exp": int((now + timedelta(minutes=settings.jwt_expire_minutes)).timestamp()),
    }
    return jwt.encode(payload, settings.jwt_secret, algorithm="HS256")


def current_user(
    authorization: Annotated[str | None, Header()] = None,
    db: Session = Depends(get_db),
) -> SysUser:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="未登录")
    settings = get_settings()
    try:
        payload = jwt.decode(authorization.removeprefix("Bearer "), settings.jwt_secret, algorithms=["HS256"])
        user_id = int(payload["sub"])
    except Exception as exc:
        raise HTTPException(status_code=401, detail="登录已失效") from exc
    user = db.get(SysUser, user_id)
    if user is None:
        raise HTTPException(status_code=401, detail="用户不存在")
    return user


def admin_user(user: SysUser = Depends(current_user)) -> SysUser:
    if user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="需要管理员权限")
    return user
