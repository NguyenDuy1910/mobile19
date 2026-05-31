from sqlalchemy import select
from sqlalchemy.orm import Session

from auth.models import User
from shared.errors import not_found
from users.models import UserProfile
from users.schemas import MeResponse, ProfileUpdate


def get_profile(db: Session, user: User) -> UserProfile:
    profile = db.scalar(select(UserProfile).where(UserProfile.user_id == user.id))
    if not profile:
        raise not_found("User profile not found")
    return profile


def get_me(db: Session, user: User) -> MeResponse:
    return MeResponse(id=user.id, email=user.email, profile=get_profile(db, user))


def update_profile(db: Session, user: User, payload: ProfileUpdate) -> UserProfile:
    profile = get_profile(db, user)
    for field, value in payload.model_dump(exclude_unset=True).items():
        setattr(profile, field, value)
    db.commit()
    db.refresh(profile)
    return profile
