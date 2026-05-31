from uuid import UUID

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from auth.dependencies import get_current_user
from auth.models import User
from database.session import get_db
from learning.schemas import (
    DailyPlanResponse,
    DueWordResponse,
    ReviewRequest,
    ReviewResponse,
    SessionEndByIdRequest,
    SessionEndRequest,
    SessionResponse,
)
from learning.service import end_session, get_daily_plan, get_due_words, review_word, start_session

router = APIRouter(prefix="/learning", tags=["learning"])


@router.get("/daily-plan", response_model=DailyPlanResponse)
def daily_plan(user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> DailyPlanResponse:
    return get_daily_plan(db, user)


@router.get("/due-words", response_model=list[DueWordResponse])
def due_words(
    limit: int | None = Query(default=None, ge=1, le=500),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[DueWordResponse]:
    return get_due_words(db, user, limit)


@router.post("/review", response_model=ReviewResponse)
def submit_review(
    payload: ReviewRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ReviewResponse:
    return review_word(db, user, payload)


@router.post("/session/start", response_model=SessionResponse, status_code=201)
def begin_session(user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> SessionResponse:
    return start_session(db, user)


@router.post("/session/end", response_model=SessionResponse)
def finish_session(
    payload: SessionEndByIdRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> SessionResponse:
    values = SessionEndRequest(**payload.model_dump(exclude={"session_id"}))
    return end_session(db, user, payload.session_id, values)


@router.post("/session/{session_id}/end", response_model=SessionResponse)
def finish_session_by_path(
    session_id: UUID,
    payload: SessionEndRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> SessionResponse:
    return end_session(db, user, session_id, payload)
