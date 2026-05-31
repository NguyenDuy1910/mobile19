from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator


def _clean_word(value: str | None) -> str | None:
    return value.strip().lower() if value is not None else None


class WordLookupRequest(BaseModel):
    word: str = Field(min_length=1, max_length=160)

    _normalize_word = field_validator("word")(_clean_word)


class DictionaryWord(BaseModel):
    word: str
    phonetic: str | None = None
    audio_url: str | None = None
    part_of_speech: str | None = None
    meaning: str
    example: str | None = None
    synonyms: list[str] = Field(default_factory=list)
    antonyms: list[str] = Field(default_factory=list)
    source: str = "dictionaryapi.dev"


class VocabItemCreate(BaseModel):
    word: str = Field(min_length=1, max_length=160)
    pronunciation: str | None = None
    meaning: str = Field(min_length=1)
    description_en: str | None = None
    example: str | None = None
    collocations: list[str] = Field(default_factory=list)
    related_words: list[str] = Field(default_factory=list)
    note: str | None = None
    part_of_speech: str | None = None
    phonetic: str | None = None
    audio_url: str | None = None
    synonyms: list[str] = Field(default_factory=list)
    antonyms: list[str] = Field(default_factory=list)
    source: str = "manual"

    _normalize_word = field_validator("word")(_clean_word)


class VocabItemUpdate(BaseModel):
    word: str | None = Field(default=None, min_length=1, max_length=160)
    pronunciation: str | None = None
    meaning: str | None = Field(default=None, min_length=1)
    description_en: str | None = None
    example: str | None = None
    collocations: list[str] | None = None
    related_words: list[str] | None = None
    note: str | None = None
    part_of_speech: str | None = None
    phonetic: str | None = None
    audio_url: str | None = None
    synonyms: list[str] | None = None
    antonyms: list[str] | None = None

    _normalize_word = field_validator("word")(_clean_word)


class WordEnrichRequest(BaseModel):
    word: str = Field(min_length=1, max_length=160)
    pronunciation: str | None = None
    meaning: str | None = Field(default=None, min_length=1)
    description_en: str | None = None
    example: str | None = None
    collocations: list[str] | None = None
    related_words: list[str] | None = None
    note: str | None = None
    part_of_speech: str | None = None
    phonetic: str | None = None
    audio_url: str | None = None
    synonyms: list[str] | None = None
    antonyms: list[str] | None = None

    _normalize_word = field_validator("word")(_clean_word)


class VocabItemResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    deck_id: UUID
    word: str
    pronunciation: str | None
    meaning: str
    description_en: str | None
    example: str | None
    collocations: list[str]
    related_words: list[str]
    note: str | None
    part_of_speech: str | None
    phonetic: str | None
    audio_url: str | None
    synonyms: list[str]
    antonyms: list[str]
    source: str
    created_at: datetime
    updated_at: datetime


class ImportRowError(BaseModel):
    row: int
    message: str


class ImportReport(BaseModel):
    imported: int
    skipped: int
    errors: list[ImportRowError]
