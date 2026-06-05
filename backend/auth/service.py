from uuid import UUID
from google.oauth2 import id_token
from google.auth.transport import requests as google_requests
from fastapi import HTTPException

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

GOOGLE_CLIENT_ID = "511181274296-fumbnrcanka77rhatia01akkqnt51tme.apps.googleusercontent.com"

def login_with_google(db: Session, token: str) -> TokenResponse:
    try:
        idinfo = id_token.verify_oauth2_token(
            token,
            google_requests.Request(),
            GOOGLE_CLIENT_ID,
        )
        email = idinfo["email"]
    except ValueError:
        raise HTTPException(status_code=401, detail={"code": "INVALID_TOKEN", "message": "Invalid Google token"})

    user = db.scalar(select(User).where(User.email == email))
    if user is None:
        user = User(
            email=email,
            password_hash="",
            provider="google",
        )
        db.add(user)
        db.flush()
        db.add(UserProfile(
            user_id=user.id,
            name=idinfo.get("name") or email.split("@")[0],
        ))
        db.commit()
        db.refresh(user)
    else:
        # User đã tồn tại, kiểm tra profile
        profile = db.scalar(select(UserProfile).where(UserProfile.user_id == user.id))
        if profile is None:
            db.add(UserProfile(
                user_id=user.id,
                name=idinfo.get("name") or email.split("@")[0],
            ))
            db.commit()

    return _token_response(user)

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
