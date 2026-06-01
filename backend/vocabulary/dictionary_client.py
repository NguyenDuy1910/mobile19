from urllib.parse import quote

import httpx

from config.settings import settings
from shared.errors import api_error
from vocabulary.schemas import DictionaryWord


def _unique(values: list[str]) -> list[str]:
    return list(dict.fromkeys(value for value in values if value))


class DictionaryClient:
    def __init__(
        self,
        base_url: str | None = None,
        timeout: float | None = None,
        transport: httpx.AsyncBaseTransport | None = None,
    ) -> None:
        self.base_url = (base_url or settings.dictionary_api_base_url).rstrip("/")
        self.timeout = timeout or settings.dictionary_api_timeout_seconds
        self.transport = transport

    async def lookup_word(self, word: str) -> DictionaryWord:
        normalized_word = word.strip().lower()
        try:
            async with httpx.AsyncClient(
                base_url=self.base_url,
                timeout=self.timeout,
                transport=self.transport,
            ) as client:
                response = await client.get(f"/entries/en/{quote(normalized_word)}")
        except httpx.TimeoutException as exc:
            raise api_error(504, "EXTERNAL_DICTIONARY_TIMEOUT", "Dictionary lookup timed out") from exc
        except httpx.RequestError as exc:
            raise api_error(502, "EXTERNAL_DICTIONARY_ERROR", "Dictionary service is unavailable") from exc

        if response.status_code == 404:
            raise api_error(404, "WORD_NOT_FOUND", "Word was not found in DictionaryAPI.dev")
        if response.status_code >= 400:
            raise api_error(502, "EXTERNAL_DICTIONARY_ERROR", "Dictionary lookup failed")

        try:
            payload = response.json()
            entry = payload[0]
            meanings = entry.get("meanings") or []
            selected_meaning = next(
                meaning
                for meaning in meanings
                if meaning.get("definitions") and meaning["definitions"][0].get("definition")
            )
            definition = selected_meaning["definitions"][0]
            phonetics = entry.get("phonetics") or []
            phonetic = entry.get("phonetic") or next(
                (item.get("text") for item in phonetics if item.get("text")),
                None,
            )
            audio_url = next((item.get("audio") for item in phonetics if item.get("audio")), None)
            synonyms = _unique((selected_meaning.get("synonyms") or []) + (definition.get("synonyms") or []))
            antonyms = _unique((selected_meaning.get("antonyms") or []) + (definition.get("antonyms") or []))
            return DictionaryWord(
                word=entry.get("word") or normalized_word,
                phonetic=phonetic,
                audio_url=audio_url,
                part_of_speech=selected_meaning.get("partOfSpeech"),
                meaning=definition["definition"],
                example=definition.get("example"),
                synonyms=synonyms,
                antonyms=antonyms,
            )
        except (IndexError, KeyError, StopIteration, TypeError, ValueError) as exc:
            raise api_error(502, "EXTERNAL_DICTIONARY_ERROR", "Dictionary returned an invalid response") from exc


dictionary_client = DictionaryClient()
