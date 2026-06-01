from datetime import date
from uuid import UUID

from pydantic import BaseModel


class ProgressSummary(BaseModel):
    learned_words: int
    streak_days: int
    accuracy_percentage: float
    due_reviews: int
    level_estimation: str


class DailyActivity(BaseModel):
    date: date
    reviews: int
    correct: int
    wrong: int


class RetentionResponse(BaseModel):
    total_reviews: int
    correct_reviews: int
    wrong_reviews: int
    retention_percentage: float


class DeckProgressResponse(BaseModel):
    deck_id: UUID
    total_words: int
    learned_words: int
    due_reviews: int
    accuracy_percentage: float
