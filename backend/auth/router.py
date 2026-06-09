from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from auth.dependencies import get_current_user
from auth.models import User
from auth.schemas import (
    GoogleLoginRequest,
    LoginRequest,
    MessageResponse,
    RefreshRequest,
    RegisterRequest,
    TokenResponse,
)

from auth.service import login, login_with_google, refresh, register
from database.session import get_db

router = APIRouter(prefix="/auth", tags=["auth"])

@router.post("/google", response_model=TokenResponse)
def google_login(payload: GoogleLoginRequest, db: Session = Depends(get_db)) -> TokenResponse:
    return login_with_google(db, payload.id_token)

@router.post("/register", response_model=TokenResponse, status_code=201)
def register_user(payload: RegisterRequest, db: Session = Depends(get_db)) -> TokenResponse:
    return register(db, payload.email, payload.password)


@router.post("/login", response_model=TokenResponse)
def login_user(payload: LoginRequest, db: Session = Depends(get_db)) -> TokenResponse:
    return login(db, payload.email, payload.password)


@router.post("/refresh", response_model=TokenResponse)
def refresh_tokens(payload: RefreshRequest, db: Session = Depends(get_db)) -> TokenResponse:
    return refresh(db, payload.refresh_token)


@router.post("/logout", response_model=MessageResponse)
def logout_user(_: User = Depends(get_current_user)) -> MessageResponse:
    return MessageResponse(message="Logged out. Discard the client tokens.")
