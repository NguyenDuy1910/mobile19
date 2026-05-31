from uuid import UUID

from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.orm import Session

from auth.dependencies import get_current_user
from auth.models import User
from database.session import get_db
from decks.schemas import DeckCreate, DeckResponse, DeckUpdate
from decks.service import create_deck, delete_deck, get_owned_deck, list_decks, update_deck

router = APIRouter(prefix="/decks", tags=["decks"])


@router.get("", response_model=list[DeckResponse])
def read_decks(user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> list[DeckResponse]:
    return list_decks(db, user)


@router.post("", response_model=DeckResponse, status_code=201)
def post_deck(
    payload: DeckCreate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> DeckResponse:
    return create_deck(db, user, payload)


@router.get("/{deck_id}", response_model=DeckResponse)
def read_deck(
    deck_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> DeckResponse:
    return get_owned_deck(db, user, deck_id)


@router.patch("/{deck_id}", response_model=DeckResponse)
def patch_deck(
    deck_id: UUID,
    payload: DeckUpdate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> DeckResponse:
    return update_deck(db, user, deck_id, payload)


@router.delete("/{deck_id}", status_code=status.HTTP_204_NO_CONTENT)
def remove_deck(
    deck_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Response:
    delete_deck(db, user, deck_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
