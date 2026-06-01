from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from uuid import UUID

from sqlalchemy import DateTime, ForeignKey, Integer, Numeric, String, UniqueConstraint, Uuid
from sqlalchemy.orm import Mapped, mapped_column

from database.base import Base, UUIDPrimaryKeyMixin
from shared.datetime import utc_now


class UserVocabProgress(UUIDPrimaryKeyMixin, Base):
    __tablename__ = "user_vocab_progress"
    __table_args__ = (UniqueConstraint("user_id", "vocab_item_id"),)

    user_id: Mapped[UUID] = mapped_column(
        Uuid,
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
    )
    vocab_item_id: Mapped[UUID] = mapped_column(
        Uuid,
        ForeignKey("vocab_items.id", ondelete="CASCADE"),
        index=True,
    )
    status: Mapped[str] = mapped_column(String(20), default="new")
    repetitions: Mapped[int] = mapped_column(Integer, default=0)
    interval_days: Mapped[int] = mapped_column(Integer, default=0)
    ease_factor: Mapped[Decimal] = mapped_column(Numeric(4, 2), default=Decimal("2.50"))
    due_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, index=True)
    last_reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    total_reviews: Mapped[int] = mapped_column(Integer, default=0)
    correct_count: Mapped[int] = mapped_column(Integer, default=0)
    wrong_count: Mapped[int] = mapped_column(Integer, default=0)


class ReviewLog(UUIDPrimaryKeyMixin, Base):
    __tablename__ = "review_logs"

    user_id: Mapped[UUID] = mapped_column(
        Uuid,
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
    )
    vocab_item_id: Mapped[UUID] = mapped_column(
        Uuid,
        ForeignKey("vocab_items.id", ondelete="CASCADE"),
        index=True,
    )
    rating: Mapped[str] = mapped_column(String(20))
    old_interval: Mapped[int] = mapped_column(Integer)
    new_interval: Mapped[int] = mapped_column(Integer)
    old_ease_factor: Mapped[Decimal] = mapped_column(Numeric(4, 2))
    new_ease_factor: Mapped[Decimal] = mapped_column(Numeric(4, 2))
    reviewed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, index=True)


class LearningSession(UUIDPrimaryKeyMixin, Base):
    __tablename__ = "learning_sessions"

    user_id: Mapped[UUID] = mapped_column(
        Uuid,
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
    )
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now)
    ended_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    new_words_count: Mapped[int] = mapped_column(Integer, default=0)
    review_words_count: Mapped[int] = mapped_column(Integer, default=0)
    correct_count: Mapped[int] = mapped_column(Integer, default=0)
    wrong_count: Mapped[int] = mapped_column(Integer, default=0)
    duration_seconds: Mapped[int | None] = mapped_column(Integer)
