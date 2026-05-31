from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session

from auth.models import User
from decks.models import VocabDeck
from decks.schemas import DeckCreate, DeckUpdate
from shared.errors import not_found


def list_decks(db: Session, user: User) -> list[VocabDeck]:
    return list(db.scalars(select(VocabDeck).where(VocabDeck.user_id == user.id).order_by(VocabDeck.created_at)))


def get_owned_deck(db: Session, user: User, deck_id: UUID) -> VocabDeck:
    deck = db.scalar(select(VocabDeck).where(VocabDeck.id == deck_id, VocabDeck.user_id == user.id))
    if not deck:
        raise not_found("Deck not found")
    return deck


def create_deck(db: Session, user: User, payload: DeckCreate) -> VocabDeck:
    deck = VocabDeck(user_id=user.id, **payload.model_dump())
    db.add(deck)
    db.commit()
    db.refresh(deck)
    return deck


def update_deck(db: Session, user: User, deck_id: UUID, payload: DeckUpdate) -> VocabDeck:
    deck = get_owned_deck(db, user, deck_id)
    for field, value in payload.model_dump(exclude_unset=True).items():
        setattr(deck, field, value)
    db.commit()
    db.refresh(deck)
    return deck


def delete_deck(db: Session, user: User, deck_id: UUID) -> None:
    deck = get_owned_deck(db, user, deck_id)
    db.delete(deck)
    db.commit()
