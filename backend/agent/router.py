from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from agent.schemas import (
    ExplainWordRequest,
    ExplainWordResponse,
    GenerateDeckRequest,
    GenerateDeckResponse,
    GenerateExamplesRequest,
    GenerateExamplesResponse,
)
from agent.service import explain_word, generate_deck, generate_examples
from auth.dependencies import get_current_user
from auth.models import User
from database.session import get_db

router = APIRouter(prefix="/agent", tags=["agent"])


@router.post("/explain-word", response_model=ExplainWordResponse)
async def post_explanation(
    payload: ExplainWordRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ExplainWordResponse:
    return await explain_word(db, user, payload.word, payload.deck_id)


@router.post("/generate-examples", response_model=GenerateExamplesResponse)
async def post_examples(
    payload: GenerateExamplesRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> GenerateExamplesResponse:
    return await generate_examples(db, user, payload.word, payload.count, payload.deck_id)


@router.post("/generate-deck", response_model=GenerateDeckResponse)
def post_deck_suggestions(
    payload: GenerateDeckRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> GenerateDeckResponse:
    return generate_deck(db, user, payload.topic, payload.limit, payload.deck_id)
