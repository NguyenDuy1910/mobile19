from __future__ import annotations

from datetime import datetime
from uuid import UUID

from sqlalchemy import JSON, DateTime, ForeignKey, String, Text, Uuid
from sqlalchemy.orm import Mapped, mapped_column

from database.base import Base, TimestampMixin, UUIDPrimaryKeyMixin
from shared.datetime import utc_now


class AgentConversation(UUIDPrimaryKeyMixin, TimestampMixin, Base):
    __tablename__ = "agent_conversations"

    user_id: Mapped[UUID] = mapped_column(
        Uuid,
        ForeignKey("users.id", ondelete="CASCADE"),
        index=True,
    )
    title: Mapped[str | None] = mapped_column(String(160))


class AgentMessage(UUIDPrimaryKeyMixin, Base):
    __tablename__ = "agent_messages"

    conversation_id: Mapped[UUID] = mapped_column(
        Uuid,
        ForeignKey("agent_conversations.id", ondelete="CASCADE"),
        index=True,
    )
    role: Mapped[str] = mapped_column(String(20))
    content: Mapped[str] = mapped_column(Text)
    message_metadata: Mapped[dict | None] = mapped_column("metadata", JSON)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utc_now)
