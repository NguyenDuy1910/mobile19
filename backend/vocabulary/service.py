from uuid import UUID

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from auth.models import User
from decks.models import VocabDeck
from decks.service import get_owned_deck
from learning.models import UserVocabProgress
from shared.errors import bad_request, not_found
from vocabulary.dictionary_client import dictionary_client
from vocabulary.models import VocabItem
from vocabulary.schemas import VocabItemCreate, VocabItemUpdate, WordEnrichRequest


def list_words(db: Session, user: User, deck_id: UUID) -> list[VocabItem]:
    get_owned_deck(db, user, deck_id)
    return list(db.scalars(select(VocabItem).where(VocabItem.deck_id == deck_id).order_by(VocabItem.created_at)))


def get_owned_word(db: Session, user: User, word_id: UUID) -> VocabItem:
    word = db.scalar(
        select(VocabItem)
        .join_from(VocabItem, VocabDeck)
        .where(VocabItem.id == word_id, VocabDeck.user_id == user.id)
    )
    if not word:
        raise not_found("Vocabulary item not found")
    return word


def create_word(db: Session, user: User, deck_id: UUID, payload: VocabItemCreate) -> VocabItem:
    get_owned_deck(db, user, deck_id)
    item = VocabItem(deck_id=deck_id, **payload.model_dump())
    db.add(item)
    db.flush()
    db.add(UserVocabProgress(user_id=user.id, vocab_item_id=item.id))
    db.commit()
    db.refresh(item)
    return item


def update_word(db: Session, user: User, word_id: UUID, payload: VocabItemUpdate) -> VocabItem:
    item = get_owned_word(db, user, word_id)
    for field, value in payload.model_dump(exclude_unset=True).items():
        setattr(item, field, value)
    db.commit()
    db.refresh(item)
    return item


def delete_word(db: Session, user: User, word_id: UUID) -> None:
    item = get_owned_word(db, user, word_id)
    db.delete(item)
    db.commit()


async def enrich_word(db: Session, user: User, deck_id: UUID, payload: WordEnrichRequest) -> VocabItem:
    get_owned_deck(db, user, deck_id)
    overrides = payload.model_dump(exclude_unset=True)
    try:
        lookup = await dictionary_client.lookup_word(payload.word)
        values = lookup.model_dump()
        values["pronunciation"] = lookup.phonetic
    except HTTPException:
        if not payload.meaning:
            raise bad_request("A manual meaning is required when dictionary enrichment fails")
        values = {"word": payload.word, "meaning": payload.meaning, "source": "manual"}

    values.update(overrides)
    values.setdefault("meaning", payload.meaning)
    if not values.get("meaning"):
        raise bad_request("Meaning is required")
    return create_word(db, user, deck_id, VocabItemCreate(**values))
