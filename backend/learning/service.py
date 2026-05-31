from decimal import Decimal
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session

from auth.models import User
from decks.models import VocabDeck
from learning.models import LearningSession, ReviewLog, UserVocabProgress
from learning.schemas import DailyPlanResponse, DueWordResponse, ReviewRequest, SessionEndRequest
from shared.datetime import ensure_utc, utc_now
from shared.errors import not_found
from srs.sm2 import calculate_sm2
from users.service import get_profile
from vocabulary.models import VocabItem
from vocabulary.service import get_owned_word


def get_due_words(db: Session, user: User, limit: int | None = None) -> list[DueWordResponse]:
    query = (
        select(UserVocabProgress, VocabItem)
        .join(VocabItem, UserVocabProgress.vocab_item_id == VocabItem.id)
        .join(VocabDeck, VocabItem.deck_id == VocabDeck.id)
        .where(
            UserVocabProgress.user_id == user.id,
            VocabDeck.user_id == user.id,
            UserVocabProgress.due_at <= utc_now(),
        )
        .order_by(UserVocabProgress.due_at)
    )
    if limit:
        query = query.limit(limit)
    return [
        DueWordResponse(
            vocab_item=item,
            status=progress.status,
            repetitions=progress.repetitions,
            interval_days=progress.interval_days,
            ease_factor=float(progress.ease_factor),
            due_at=progress.due_at,
        )
        for progress, item in db.execute(query).all()
    ]


def get_daily_plan(db: Session, user: User) -> DailyPlanResponse:
    profile = get_profile(db, user)
    due_words = get_due_words(db, user)
    new_words_due = min(sum(item.status == "new" for item in due_words), profile.daily_new_words)
    reviews_due = min(sum(item.status != "new" for item in due_words), profile.daily_review_limit)
    return DailyPlanResponse(
        new_words_target=profile.daily_new_words,
        review_limit=profile.daily_review_limit,
        new_words_due=new_words_due,
        reviews_due=reviews_due,
        total_due=new_words_due + reviews_due,
    )


def review_word(db: Session, user: User, payload: ReviewRequest) -> UserVocabProgress:
    get_owned_word(db, user, payload.vocab_item_id)
    progress = db.scalar(
        select(UserVocabProgress).where(
            UserVocabProgress.user_id == user.id,
            UserVocabProgress.vocab_item_id == payload.vocab_item_id,
        )
    )
    if not progress:
        progress = UserVocabProgress(user_id=user.id, vocab_item_id=payload.vocab_item_id)
        db.add(progress)
        db.flush()

    old_interval = progress.interval_days
    old_ease = progress.ease_factor
    result = calculate_sm2(progress.repetitions, progress.interval_days, float(progress.ease_factor), payload.rating)
    progress.status = result.status
    progress.repetitions = result.repetitions
    progress.interval_days = result.interval_days
    progress.ease_factor = Decimal(str(result.ease_factor))
    progress.due_at = result.due_at
    progress.last_reviewed_at = utc_now()
    progress.total_reviews += 1
    if payload.rating == "again":
        progress.wrong_count += 1
    else:
        progress.correct_count += 1
    db.add(
        ReviewLog(
            user_id=user.id,
            vocab_item_id=payload.vocab_item_id,
            rating=payload.rating,
            old_interval=old_interval,
            new_interval=result.interval_days,
            old_ease_factor=old_ease,
            new_ease_factor=progress.ease_factor,
        )
    )
    db.commit()
    db.refresh(progress)
    return progress


def start_session(db: Session, user: User) -> LearningSession:
    session = LearningSession(user_id=user.id)
    db.add(session)
    db.commit()
    db.refresh(session)
    return session


def end_session(db: Session, user: User, session_id: UUID, payload: SessionEndRequest) -> LearningSession:
    session = db.scalar(
        select(LearningSession).where(LearningSession.id == session_id, LearningSession.user_id == user.id)
    )
    if not session:
        raise not_found("Learning session not found")
    if not session.ended_at:
        session.ended_at = utc_now()
        session.duration_seconds = max(0, int((session.ended_at - ensure_utc(session.started_at)).total_seconds()))
    for field, value in payload.model_dump().items():
        setattr(session, field, value)
    db.commit()
    db.refresh(session)
    return session
