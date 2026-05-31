from uuid import UUID

from fastapi import status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from auth.models import User
from auth.schemas import TokenResponse
from config.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    verify_password,
)
from shared.errors import api_error
from shared.utils import normalize_email
from users.models import UserProfile


def _token_response(user: User) -> TokenResponse:
    return TokenResponse(
        access_token=create_access_token(user.id),
        refresh_token=create_refresh_token(user.id),
        user=user,
    )


def register(db: Session, email: str, password: str) -> TokenResponse:
    normalized_email = normalize_email(email)
    if db.scalar(select(User).where(User.email == normalized_email)):
        raise api_error(status.HTTP_409_CONFLICT, "DUPLICATE_EMAIL", "Email is already registered")

    user = User(email=normalized_email, password_hash=hash_password(password))
    db.add(user)
    db.flush()
    db.add(UserProfile(user_id=user.id, name=normalized_email.split("@", maxsplit=1)[0]))
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise api_error(status.HTTP_409_CONFLICT, "DUPLICATE_EMAIL", "Email is already registered") from exc
    db.refresh(user)
    return _token_response(user)


def login(db: Session, email: str, password: str) -> TokenResponse:
    user = db.scalar(select(User).where(User.email == normalize_email(email)))
    if not user or not verify_password(password, user.password_hash):
        raise api_error(status.HTTP_401_UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password")
    return _token_response(user)


def refresh(db: Session, refresh_token: str) -> TokenResponse:
    payload = decode_token(refresh_token, "refresh")
    user = db.get(User, UUID(payload["sub"]))
    if not user:
        raise api_error(status.HTTP_401_UNAUTHORIZED, "UNAUTHORIZED", "User no longer exists")
    return _token_response(user)
