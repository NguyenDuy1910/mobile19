from datetime import datetime
from typing import Literal

from pydantic import BaseModel

ReviewRating = Literal["again", "hard", "good", "easy"]


class SM2Result(BaseModel):
    status: str
    repetitions: int
    interval_days: int
    ease_factor: float
    due_at: datetime
