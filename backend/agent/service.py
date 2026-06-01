from uuid import UUID

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from agent.prompts import DEFAULT_COLLOCATION_TEMPLATES, TOPIC_WORDS
from agent.schemas import (
    DeckSuggestion,
    ExplainWordResponse,
    GenerateDeckResponse,
    GenerateExamplesResponse,
)
from auth.models import User
from decks.models import VocabDeck
from decks.service import get_owned_deck
from users.service import get_profile
from vocabulary.dictionary_client import dictionary_client
from vocabulary.models import VocabItem


def _profile_context(db: Session, user: User) -> tuple[str, str, int]:
    profile = get_profile(db, user)
    return profile.english_level or "B1", profile.learning_goal or "General English", profile.daily_new_words


def _verify_deck(db: Session, user: User, deck_id: UUID | None) -> None:
    if deck_id:
        get_owned_deck(db, user, deck_id)


def _local_word(db: Session, user: User, word: str) -> VocabItem | None:
    return db.scalar(
        select(VocabItem)
        .join(VocabDeck, VocabItem.deck_id == VocabDeck.id)
        .where(VocabDeck.user_id == user.id, VocabItem.word == word.strip().lower())
        .limit(1)
    )


async def explain_word(db: Session, user: User, word: str, deck_id: UUID | None) -> ExplainWordResponse:
    _verify_deck(db, user, deck_id)
    level, goal, daily_target = _profile_context(db, user)
    try:
        lookup = await dictionary_client.lookup_word(word)
        explanation = lookup.meaning
        examples = [lookup.example] if lookup.example else []
    except HTTPException:
        local = _local_word(db, user, word)
        if not local:
            raise
        explanation = local.meaning
        examples = [local.example] if local.example else []
    if not examples:
        examples = [
            f"Use '{word.strip().lower()}' in a sentence for your {daily_target}-word daily {goal} goal."
        ]
    collocations = [template.format(word=word.strip().lower()) for template in DEFAULT_COLLOCATION_TEMPLATES]
    return ExplainWordResponse(
        word=word.strip().lower(),
        simple_explanation=explanation,
        level=level,
        goal=goal,
        examples=examples,
        collocations=collocations,
    )


async def generate_examples(
    db: Session,
    user: User,
    word: str,
    count: int,
    deck_id: UUID | None,
) -> GenerateExamplesResponse:
    explanation = await explain_word(db, user, word, deck_id)
    examples = list(explanation.examples)
    while len(examples) < count:
        examples.append(
            f"Example {len(examples) + 1}: I practice '{explanation.word}' for my {explanation.goal} vocabulary."
        )
    return GenerateExamplesResponse(
        word=explanation.word,
        examples=examples[:count],
        level=explanation.level,
        goal=explanation.goal,
    )


def generate_deck(
    db: Session,
    user: User,
    topic: str,
    limit: int,
    deck_id: UUID | None,
) -> GenerateDeckResponse:
    _verify_deck(db, user, deck_id)
    level, goal, daily_target = _profile_context(db, user)
    normalized_topic = topic.strip().lower()
    words = TOPIC_WORDS.get(normalized_topic, TOPIC_WORDS["ielts"])
    suggestions = [
        DeckSuggestion(word=word, reason=f"Useful {normalized_topic} vocabulary for a {level} learner.")
        for word in words[: min(limit, daily_target)]
    ]
    return GenerateDeckResponse(topic=topic.strip(), suggestions=suggestions, level=level, goal=goal)
