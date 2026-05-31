from __future__ import annotations

from uuid import UUID

from sqlalchemy import JSON, ForeignKey, String, Text, Uuid
from sqlalchemy.orm import Mapped, mapped_column

from database.base import Base, TimestampMixin, UUIDPrimaryKeyMixin


class VocabItem(UUIDPrimaryKeyMixin, TimestampMixin, Base):
    __tablename__ = "vocab_items"

    deck_id: Mapped[UUID] = mapped_column(
        Uuid,
        ForeignKey("vocab_decks.id", ondelete="CASCADE"),
        index=True,
    )
    word: Mapped[str] = mapped_column(String(160), index=True)
    pronunciation: Mapped[str | None] = mapped_column(String(255))
    meaning: Mapped[str] = mapped_column(Text)
    description_en: Mapped[str | None] = mapped_column(Text)
    example: Mapped[str | None] = mapped_column(Text)
    collocations: Mapped[list[str]] = mapped_column(JSON, default=list)
    related_words: Mapped[list[str]] = mapped_column(JSON, default=list)
    note: Mapped[str | None] = mapped_column(Text)
    part_of_speech: Mapped[str | None] = mapped_column(String(80))
    phonetic: Mapped[str | None] = mapped_column(String(255))
    audio_url: Mapped[str | None] = mapped_column(Text)
    synonyms: Mapped[list[str]] = mapped_column(JSON, default=list)
    antonyms: Mapped[list[str]] = mapped_column(JSON, default=list)
    source: Mapped[str] = mapped_column(String(80), default="manual")
