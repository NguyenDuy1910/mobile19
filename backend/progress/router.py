from uuid import UUID

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from auth.dependencies import get_current_user
from auth.models import User
from database.session import get_db
from progress.schemas import DailyActivity, DeckProgressResponse, ProgressSummary, RetentionResponse
from progress.service import daily_activity, deck_progress, progress_summary, retention

router = APIRouter(prefix="/progress", tags=["progress"])


@router.get("/summary", response_model=ProgressSummary)
def read_summary(user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> ProgressSummary:
    return progress_summary(db, user)


@router.get("/daily-activity", response_model=list[DailyActivity])
def read_activity(user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> list[DailyActivity]:
    return daily_activity(db, user)


@router.get("/retention", response_model=RetentionResponse)
def read_retention(user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> RetentionResponse:
    return retention(db, user)


@router.get("/decks/{deck_id}", response_model=DeckProgressResponse)
def read_deck_progress(
    deck_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> DeckProgressResponse:
    return deck_progress(db, user, deck_id)
