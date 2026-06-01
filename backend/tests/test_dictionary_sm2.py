from datetime import timedelta

import httpx
import pytest

from shared.datetime import utc_now
from srs.sm2 import calculate_sm2
from vocabulary.dictionary_client import DictionaryClient


@pytest.mark.asyncio
async def test_dictionary_client_normalizes_response():
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path.endswith("/entries/en/resilient")
        return httpx.Response(
            200,
            json=[
                {
                    "word": "resilient",
                    "phonetic": "/rɪˈzɪliənt/",
                    "phonetics": [{"audio": "https://audio.example/resilient.mp3"}],
                    "meanings": [
                        {
                            "partOfSpeech": "adjective",
                            "synonyms": ["strong"],
                            "antonyms": ["fragile"],
                            "definitions": [
                                {
                                    "definition": "Able to recover quickly.",
                                    "example": "A resilient team adapts.",
                                }
                            ],
                        }
                    ],
                }
            ],
        )

    client = DictionaryClient(transport=httpx.MockTransport(handler))
    word = await client.lookup_word("resilient")
    assert word.meaning == "Able to recover quickly."
    assert word.audio_url == "https://audio.example/resilient.mp3"
    assert word.synonyms == ["strong"]


@pytest.mark.asyncio
async def test_dictionary_client_maps_not_found_and_timeout():
    not_found = DictionaryClient(transport=httpx.MockTransport(lambda _: httpx.Response(404)))
    with pytest.raises(Exception) as missing:
        await not_found.lookup_word("missing")
    assert missing.value.detail["code"] == "WORD_NOT_FOUND"

    def timeout(request: httpx.Request) -> httpx.Response:
        raise httpx.ReadTimeout("slow", request=request)

    timed_out = DictionaryClient(transport=httpx.MockTransport(timeout))
    with pytest.raises(Exception) as delayed:
        await timed_out.lookup_word("slow")
    assert delayed.value.detail["code"] == "EXTERNAL_DICTIONARY_TIMEOUT"


def test_sm2_ratings_and_due_dates():
    before = utc_now()
    again = calculate_sm2(2, 6, 2.5, "again")
    assert again.status == "learning"
    assert again.repetitions == 0
    assert before + timedelta(minutes=9) < again.due_at < before + timedelta(minutes=11)

    hard = calculate_sm2(0, 0, 2.5, "hard")
    good = calculate_sm2(1, 1, 2.5, "good")
    easy = calculate_sm2(2, 6, 2.5, "easy")
    assert hard.interval_days == 1
    assert hard.ease_factor < 2.5
    assert good.interval_days == 6
    assert easy.interval_days == 16
    assert easy.ease_factor > 2.5

    floor = calculate_sm2(5, 10, 1.3, "again")
    assert floor.ease_factor == 1.3
