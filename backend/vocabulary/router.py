from uuid import UUID

from fastapi import APIRouter, Depends, File, Response, UploadFile, status
from sqlalchemy.orm import Session

from auth.dependencies import get_current_user
from auth.models import User
from database.session import get_db
from vocabulary.dictionary_client import dictionary_client
from vocabulary.import_export import export_csv, import_csv
from vocabulary.schemas import (
    DictionaryWord,
    ImportReport,
    VocabItemCreate,
    VocabItemResponse,
    VocabItemUpdate,
    WordEnrichRequest,
    WordLookupRequest,
)
from vocabulary.service import create_word, delete_word, enrich_word, list_words, update_word

router = APIRouter(tags=["vocabulary"])


@router.get("/decks/{deck_id}/words", response_model=list[VocabItemResponse])
def read_words(
    deck_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[VocabItemResponse]:
    return list_words(db, user, deck_id)


@router.post("/decks/{deck_id}/words", response_model=VocabItemResponse, status_code=201)
def post_word(
    deck_id: UUID,
    payload: VocabItemCreate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> VocabItemResponse:
    return create_word(db, user, deck_id, payload)


@router.patch("/words/{word_id}", response_model=VocabItemResponse)
def patch_word(
    word_id: UUID,
    payload: VocabItemUpdate,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> VocabItemResponse:
    return update_word(db, user, word_id, payload)


@router.delete("/words/{word_id}", status_code=status.HTTP_204_NO_CONTENT)
def remove_word(
    word_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Response:
    delete_word(db, user, word_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.post("/words/lookup", response_model=DictionaryWord)
async def lookup_word(payload: WordLookupRequest, _: User = Depends(get_current_user)) -> DictionaryWord:
    return await dictionary_client.lookup_word(payload.word)


@router.post("/decks/{deck_id}/words/enrich", response_model=VocabItemResponse, status_code=201)
async def post_enriched_word(
    deck_id: UUID,
    payload: WordEnrichRequest,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> VocabItemResponse:
    return await enrich_word(db, user, deck_id, payload)


@router.post("/decks/{deck_id}/import", response_model=ImportReport)
async def import_words(
    deck_id: UUID,
    file: UploadFile = File(...),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ImportReport:
    return import_csv(db, user, deck_id, await file.read())


@router.get("/decks/{deck_id}/export")
def export_words(
    deck_id: UUID,
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Response:
    content = export_csv(db, user, deck_id)
    return Response(
        content=content,
        media_type="text/csv",
        headers={"Content-Disposition": f'attachment; filename="minlish-{deck_id}.csv"'},
    )
