from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class PracticeGenerateRequest(BaseModel):
    limit: int = Field(default=5, ge=1, le=20)


class PracticeQuestion(BaseModel):
    vocab_item_id: UUID
    question_type: str
    question: str
    options: list[str]


class PracticeSubmitRequest(BaseModel):
    vocab_item_id: UUID
    user_answer: str = Field(min_length=1)


class PracticeAnswerResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    vocab_item_id: UUID
    question_type: str
    question: str
    user_answer: str
    correct_answer: str
    is_correct: bool
    ai_feedback: str | None
    created_at: datetime
