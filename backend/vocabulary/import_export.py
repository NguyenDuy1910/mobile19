import csv
import io
from uuid import UUID

from pydantic import ValidationError
from sqlalchemy.orm import Session

from auth.models import User
from decks.service import get_owned_deck
from vocabulary.schemas import ImportReport, ImportRowError, VocabItemCreate
from vocabulary.service import create_word, list_words

CSV_COLUMNS = [
    "word",
    "pronunciation",
    "meaning",
    "description_en",
    "example",
    "collocations",
    "related_words",
    "note",
]


def _split_values(value: str | None) -> list[str]:
    return [item.strip() for item in (value or "").split(";") if item.strip()]


def import_csv(db: Session, user: User, deck_id: UUID, content: bytes) -> ImportReport:
    get_owned_deck(db, user, deck_id)
    errors: list[ImportRowError] = []
    imported = 0
    try:
        reader = csv.DictReader(io.StringIO(content.decode("utf-8-sig")))
        if not reader.fieldnames or not {"word", "meaning"}.issubset(reader.fieldnames):
            return ImportReport(
                imported=0,
                skipped=1,
                errors=[ImportRowError(row=1, message="CSV must include word and meaning columns")],
            )
        for row_number, row in enumerate(reader, start=2):
            if not (row.get("word") or "").strip() or not (row.get("meaning") or "").strip():
                errors.append(ImportRowError(row=row_number, message="word and meaning are required"))
                continue
            try:
                payload = VocabItemCreate(
                    word=row["word"],
                    pronunciation=row.get("pronunciation") or None,
                    meaning=row["meaning"],
                    description_en=row.get("description_en") or None,
                    example=row.get("example") or None,
                    collocations=_split_values(row.get("collocations")),
                    related_words=_split_values(row.get("related_words")),
                    note=row.get("note") or None,
                    source="csv",
                )
            except ValidationError as exc:
                errors.append(ImportRowError(row=row_number, message=str(exc.errors()[0]["msg"])))
                continue
            create_word(db, user, deck_id, payload)
            imported += 1
    except (UnicodeDecodeError, csv.Error):
        errors.append(ImportRowError(row=1, message="CSV must be UTF-8 encoded"))
    return ImportReport(imported=imported, skipped=len(errors), errors=errors)


def export_csv(db: Session, user: User, deck_id: UUID) -> str:
    get_owned_deck(db, user, deck_id)
    output = io.StringIO()
    writer = csv.DictWriter(output, fieldnames=CSV_COLUMNS)
    writer.writeheader()
    for item in list_words(db, user, deck_id):
        writer.writerow(
            {
                "word": item.word,
                "pronunciation": item.pronunciation or "",
                "meaning": item.meaning,
                "description_en": item.description_en or "",
                "example": item.example or "",
                "collocations": ";".join(item.collocations),
                "related_words": ";".join(item.related_words),
                "note": item.note or "",
            }
        )
    return output.getvalue()
