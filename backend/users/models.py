from __future__ import annotations

from datetime import time
from uuid import UUID

from sqlalchemy import Boolean, ForeignKey, Integer, String, Time, Uuid
from sqlalchemy.orm import Mapped, mapped_column

from database.base import Base, TimestampMixin, UUIDPrimaryKeyMixin


class UserProfile(UUIDPrimaryKeyMixin, TimestampMixin, Base):
    __tablename__ = "user_profiles"

    user_id: Mapped[UUID] = mapped_column(
        Uuid,
        ForeignKey("users.id", ondelete="CASCADE"),
        unique=True,
        index=True,
    )
    name: Mapped[str] = mapped_column(String(120), default="")
    learning_goal: Mapped[str | None] = mapped_column(String(120))
    english_level: Mapped[str | None] = mapped_column(String(20))
    daily_new_words: Mapped[int] = mapped_column(Integer, default=10)
    daily_review_limit: Mapped[int] = mapped_column(Integer, default=50)
    notification_time: Mapped[time | None] = mapped_column(Time)
    timezone: Mapped[str] = mapped_column(String(80), default="UTC")
    notifications_enabled: Mapped[bool] = mapped_column(Boolean, default=True)
