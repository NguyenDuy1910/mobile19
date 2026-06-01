from uuid import UUID

from pydantic import BaseModel, Field


class ExplainWordRequest(BaseModel):
    word: str = Field(min_length=1, max_length=160)
    deck_id: UUID | None = None


class ExplainWordResponse(BaseModel):
    word: str
    simple_explanation: str
    level: str
    goal: str
    examples: list[str]
    collocations: list[str]


class GenerateExamplesRequest(BaseModel):
    word: str = Field(min_length=1, max_length=160)
    count: int = Field(default=3, ge=1, le=5)
    deck_id: UUID | None = None


class GenerateExamplesResponse(BaseModel):
    word: str
    examples: list[str]
    level: str
    goal: str


class GenerateDeckRequest(BaseModel):
    topic: str = Field(min_length=1, max_length=120)
    limit: int = Field(default=5, ge=1, le=20)
    deck_id: UUID | None = None


class DeckSuggestion(BaseModel):
    word: str
    reason: str


class GenerateDeckResponse(BaseModel):
    topic: str
    suggestions: list[DeckSuggestion]
    level: str
    goal: str
