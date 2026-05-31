from datetime import timedelta
from typing import Any
from uuid import UUID, uuid4

import bcrypt
from jose import JWTError, jwt

from config.settings import settings
from shared.datetime import utc_now
from shared.errors import api_error


ACCESS_TOKEN_EXPIRE_DELTA = timedelta(minutes=settings.jwt_access_token_expire_minutes)
REFRESH_TOKEN_EXPIRE_DELTA = timedelta(days=settings.jwt_refresh_token_expire_days)
JWT_ALGORITHM = "HS256"


def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


def verify_password(password: str, password_hash: str) -> bool:
    return bcrypt.checkpw(password.encode("utf-8"), password_hash.encode("utf-8"))


def create_token(user_id: UUID, token_type: str, expires_delta: timedelta) -> str:
    now = utc_now()
    payload = {
        "sub": str(user_id),
        "type": token_type,
        "jti": str(uuid4()),
        "iat": now,
        "exp": now + expires_delta,
    }
    return jwt.encode(payload, settings.jwt_secret_key, algorithm=JWT_ALGORITHM)


def create_access_token(user_id: UUID) -> str:
    return create_token(user_id, "access", ACCESS_TOKEN_EXPIRE_DELTA)


def create_refresh_token(user_id: UUID) -> str:
    return create_token(user_id, "refresh", REFRESH_TOKEN_EXPIRE_DELTA)


def decode_token(token: str, expected_type: str) -> dict[str, Any]:
    try:
        payload = jwt.decode(token, settings.jwt_secret_key, algorithms=[JWT_ALGORITHM])
        if payload.get("type") != expected_type or not payload.get("sub"):
            raise JWTError("Invalid token type")
        UUID(payload["sub"])
        return payload
    except (JWTError, ValueError) as exc:
        raise api_error(401, "UNAUTHORIZED", "Invalid or expired token") from exc
