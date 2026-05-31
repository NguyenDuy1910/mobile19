from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from auth.dependencies import get_current_user
from auth.models import User
from database.session import get_db
from users.schemas import MeResponse, ProfileResponse, ProfileUpdate
from users.service import get_me, update_profile

router = APIRouter(tags=["users"])


@router.get("/me", response_model=MeResponse)
def read_me(user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> MeResponse:
    return get_me(db, user)


@router.patch("/me/profile", response_model=ProfileResponse)
def patch_profile(
    payload: ProfileUpdate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ProfileResponse:
    return update_profile(db, user, payload)
