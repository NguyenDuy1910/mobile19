from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class DeckCreate(BaseModel):
    name: str = Field(min_length=1, max_length=160)
    description: str | None = None
    tags: list[str] = Field(default_factory=list)


class DeckUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=160)
    description: str | None = None
    tags: list[str] | None = None


class DeckResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    name: str
    description: str | None
    tags: list[str]
    created_at: datetime
    updated_at: datetime
