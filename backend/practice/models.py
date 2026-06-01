from __future__ import annotations

from datetime import datetime
from uuid import UUID

from sqlalchemy import Boolean, DateTime, ForeignKey, String, Text, Uuid
from sqlalchemy.orm import Mapped, mapped_column

from database.base import Base, UUIDPrimaryKeyMixin
from shared.datetime import utc_now


class PracticeAnswer(UUIDPrimaryKeyMixin, Base):
    __tablename__ = "practice_answers"

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
    question_type: Mapped[str] = mapped_column(String(40))
    question: Mapped[str] = mapped_column(Text)
    user_answer: Mapped[str] = mapped_column(Text)
    correct_answer: Mapped[str] = mapped_column(Text)
    is_correct: Mapped[bool] = mapped_column(Boolean)
    ai_feedback: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now, index=True)
