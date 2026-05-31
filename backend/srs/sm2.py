from datetime import timedelta

from shared.datetime import utc_now
from srs.schemas import ReviewRating, SM2Result

QUALITY_BY_RATING: dict[ReviewRating, int] = {
    "again": 1,
    "hard": 3,
    "good": 4,
    "easy": 5,
}


def calculate_sm2(
    repetitions: int,
    interval_days: int,
    ease_factor: float,
    rating: ReviewRating,
) -> SM2Result:
    now = utc_now()
    quality = QUALITY_BY_RATING[rating]
    next_ease = max(1.3, ease_factor + 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))

    if rating == "again":
        return SM2Result(
            status="learning",
            repetitions=0,
            interval_days=0,
            ease_factor=round(next_ease, 2),
            due_at=now + timedelta(minutes=10),
        )

    next_repetitions = repetitions + 1
    if next_repetitions == 1:
        next_interval = 1
    elif next_repetitions == 2:
        next_interval = 6
    else:
        next_interval = max(1, round(interval_days * next_ease))

    return SM2Result(
        status="review",
        repetitions=next_repetitions,
        interval_days=next_interval,
        ease_factor=round(next_ease, 2),
        due_at=now + timedelta(days=next_interval),
    )
