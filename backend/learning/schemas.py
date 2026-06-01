from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from srs.schemas import ReviewRating
from vocabulary.schemas import VocabItemResponse


class DueWordResponse(BaseModel):
    vocab_item: VocabItemResponse
    status: str
    repetitions: int
    interval_days: int
    ease_factor: float
    due_at: datetime


class DailyPlanResponse(BaseModel):
    new_words_target: int
    review_limit: int
    new_words_due: int
    reviews_due: int
    total_due: int


class ReviewRequest(BaseModel):
    vocab_item_id: UUID
    rating: ReviewRating


class ReviewResponse(BaseModel):
    status: str
    repetitions: int
    interval_days: int
    ease_factor: float
    due_at: datetime


class SessionEndRequest(BaseModel):
    new_words_count: int = Field(default=0, ge=0)
    review_words_count: int = Field(default=0, ge=0)
    correct_count: int = Field(default=0, ge=0)
    wrong_count: int = Field(default=0, ge=0)


class SessionEndByIdRequest(SessionEndRequest):
    session_id: UUID


class SessionResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    started_at: datetime
    ended_at: datetime | None
    new_words_count: int
    review_words_count: int
    correct_count: int
    wrong_count: int
    duration_seconds: int | None
