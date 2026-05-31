from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from auth.dependencies import get_current_user
from auth.models import User
from database.session import get_db
from practice.schemas import (
    PracticeAnswerResponse,
    PracticeGenerateRequest,
    PracticeQuestion,
    PracticeSubmitRequest,
)
from practice.service import answer_history, generate_questions, submit_answer

router = APIRouter(prefix="/practice", tags=["practice"])


@router.post("/generate", response_model=list[PracticeQuestion])
def generate_practice(
    payload: PracticeGenerateRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[PracticeQuestion]:
    return generate_questions(db, user, payload.limit)


@router.post("/submit", response_model=PracticeAnswerResponse, status_code=201)
def submit_practice(
    payload: PracticeSubmitRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> PracticeAnswerResponse:
    return submit_answer(db, user, payload)


@router.get("/history", response_model=list[PracticeAnswerResponse])
def practice_history(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[PracticeAnswerResponse]:
    return answer_history(db, user)
