from datetime import datetime, time
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class ProfileUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=120)
    learning_goal: str | None = Field(default=None, max_length=120)
    english_level: str | None = Field(default=None, max_length=20)
    daily_new_words: int | None = Field(default=None, ge=1, le=100)
    daily_review_limit: int | None = Field(default=None, ge=1, le=500)
    notification_time: time | None = None
    timezone: str | None = Field(default=None, min_length=1, max_length=80)


class ProfileResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    name: str
    learning_goal: str | None
    english_level: str | None
    daily_new_words: int
    daily_review_limit: int
    notification_time: time | None
    timezone: str
    notifications_enabled: bool
    created_at: datetime
    updated_at: datetime


class MeResponse(BaseModel):
    id: UUID
    email: str
    profile: ProfileResponse
