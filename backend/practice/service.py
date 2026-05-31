import random

from sqlalchemy import select
from sqlalchemy.orm import Session

from auth.models import User
from decks.models import VocabDeck
from practice.models import PracticeAnswer
from practice.schemas import PracticeQuestion, PracticeSubmitRequest
from vocabulary.models import VocabItem
from vocabulary.service import get_owned_word

DEFAULT_DISTRACTORS = [
    "A different vocabulary meaning.",
    "An unrelated definition.",
    "None of the listed meanings.",
]


def _owned_words(db: Session, user: User) -> list[VocabItem]:
    return list(
        db.scalars(
            select(VocabItem)
            .join(VocabDeck, VocabItem.deck_id == VocabDeck.id)
            .where(VocabDeck.user_id == user.id)
            .order_by(VocabItem.created_at)
        )
    )


def generate_questions(db: Session, user: User, limit: int) -> list[PracticeQuestion]:
    words = _owned_words(db, user)
    meanings = list(dict.fromkeys(word.meaning for word in words))
    randomizer = random.Random(str(user.id))
    questions: list[PracticeQuestion] = []
    for word in words[:limit]:
        distractors = [meaning for meaning in meanings if meaning != word.meaning]
        distractors.extend(
            value for value in DEFAULT_DISTRACTORS if value != word.meaning and value not in distractors
        )
        while len(distractors) < 3:
            distractors.append(f"Alternative meaning {len(distractors) + 1}.")
        options = [word.meaning, *randomizer.sample(distractors, 3)]
        randomizer.shuffle(options)
        questions.append(
            PracticeQuestion(
                vocab_item_id=word.id,
                question_type="word_meaning",
                question=f"What is the meaning of '{word.word}'?",
                options=options,
            )
        )
    return questions


def submit_answer(db: Session, user: User, payload: PracticeSubmitRequest) -> PracticeAnswer:
    word = get_owned_word(db, user, payload.vocab_item_id)
    is_correct = payload.user_answer.strip().casefold() == word.meaning.strip().casefold()
    answer = PracticeAnswer(
        user_id=user.id,
        vocab_item_id=word.id,
        question_type="word_meaning",
        question=f"What is the meaning of '{word.word}'?",
        user_answer=payload.user_answer,
        correct_answer=word.meaning,
        is_correct=is_correct,
        ai_feedback="Correct." if is_correct else f"Review '{word.word}' and try again.",
    )
    db.add(answer)
    db.commit()
    db.refresh(answer)
    return answer


def answer_history(db: Session, user: User) -> list[PracticeAnswer]:
    return list(
        db.scalars(
            select(PracticeAnswer)
            .where(PracticeAnswer.user_id == user.id)
            .order_by(PracticeAnswer.created_at.desc())
        )
    )
