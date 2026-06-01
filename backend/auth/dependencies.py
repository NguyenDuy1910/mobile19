from uuid import UUID

from fastapi import Depends, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from auth.models import User
from config.security import decode_token
from database.session import get_db
from shared.errors import api_error

bearer_scheme = HTTPBearer(auto_error=False)


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User:
    if not credentials or credentials.scheme.lower() != "bearer":
        raise api_error(status.HTTP_401_UNAUTHORIZED, "UNAUTHORIZED", "Authentication required")
    payload = decode_token(credentials.credentials, "access")
    user = db.get(User, UUID(payload["sub"]))
    if not user:
        raise api_error(status.HTTP_401_UNAUTHORIZED, "UNAUTHORIZED", "User no longer exists")
    return user
