from collections import defaultdict
from datetime import timedelta
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session

from auth.models import User
from decks.service import get_owned_deck
from learning.models import ReviewLog, UserVocabProgress
from progress.schemas import DailyActivity, DeckProgressResponse, ProgressSummary, RetentionResponse
from shared.datetime import ensure_utc, utc_now
from vocabulary.models import VocabItem


def _accuracy(correct: int, wrong: int) -> float:
    total = correct + wrong
    return round(correct * 100 / total, 1) if total else 0.0


def _level(learned_words: int, accuracy: float) -> str:
    if learned_words < 300 or accuracy < 60:
        return "Beginner"
    if learned_words > 1500 and accuracy > 85:
        return "Advanced"
    return "Intermediate"


def _streak(logs: list[ReviewLog]) -> int:
    reviewed_dates = {ensure_utc(log.reviewed_at).date() for log in logs}
    current = utc_now().date()
    if current not in reviewed_dates:
        current -= timedelta(days=1)
    streak = 0
    while current in reviewed_dates:
        streak += 1
        current -= timedelta(days=1)
    return streak


def progress_summary(db: Session, user: User) -> ProgressSummary:
    progresses = list(db.scalars(select(UserVocabProgress).where(UserVocabProgress.user_id == user.id)))
    logs = list(db.scalars(select(ReviewLog).where(ReviewLog.user_id == user.id)))
    correct = sum(progress.correct_count for progress in progresses)
    wrong = sum(progress.wrong_count for progress in progresses)
    accuracy = _accuracy(correct, wrong)
    learned = sum(progress.status != "new" for progress in progresses)
    due = sum(ensure_utc(progress.due_at) <= utc_now() for progress in progresses)
    return ProgressSummary(
        learned_words=learned,
        streak_days=_streak(logs),
        accuracy_percentage=accuracy,
        due_reviews=due,
        level_estimation=_level(learned, accuracy),
    )


def daily_activity(db: Session, user: User) -> list[DailyActivity]:
    grouped: dict = defaultdict(lambda: {"reviews": 0, "correct": 0, "wrong": 0})
    for log in db.scalars(select(ReviewLog).where(ReviewLog.user_id == user.id)):
        day = ensure_utc(log.reviewed_at).date()
        grouped[day]["reviews"] += 1
        grouped[day]["wrong" if log.rating == "again" else "correct"] += 1
    return [DailyActivity(date=day, **grouped[day]) for day in sorted(grouped, reverse=True)]


def retention(db: Session, user: User) -> RetentionResponse:
    progresses = list(db.scalars(select(UserVocabProgress).where(UserVocabProgress.user_id == user.id)))
    correct = sum(progress.correct_count for progress in progresses)
    wrong = sum(progress.wrong_count for progress in progresses)
    return RetentionResponse(
        total_reviews=correct + wrong,
        correct_reviews=correct,
        wrong_reviews=wrong,
        retention_percentage=_accuracy(correct, wrong),
    )


def deck_progress(db: Session, user: User, deck_id: UUID) -> DeckProgressResponse:
    get_owned_deck(db, user, deck_id)
    progresses = list(
        db.scalars(
            select(UserVocabProgress)
            .join(VocabItem, UserVocabProgress.vocab_item_id == VocabItem.id)
            .where(UserVocabProgress.user_id == user.id, VocabItem.deck_id == deck_id)
        )
    )
    correct = sum(progress.correct_count for progress in progresses)
    wrong = sum(progress.wrong_count for progress in progresses)
    return DeckProgressResponse(
        deck_id=deck_id,
        total_words=len(progresses),
        learned_words=sum(progress.status != "new" for progress in progresses),
        due_reviews=sum(ensure_utc(progress.due_at) <= utc_now() for progress in progresses),
        accuracy_percentage=_accuracy(correct, wrong),
    )
