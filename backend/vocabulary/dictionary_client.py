from urllib.parse import quote
import json
import httpx

from config.settings import settings
from shared.errors import api_error
from vocabulary.schemas import DictionaryWord


def _unique(values: list[str]) -> list[str]:
    return list(dict.fromkeys(value for value in values if value))


async def _translate_vi(text: str) -> str:
    """Dịch sang tiếng Việt qua Google Translate."""
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.get(
                "https://translate.googleapis.com/translate_a/single",
                params={"client": "gtx", "sl": "en", "tl": "vi", "dt": "t", "q": text},
                headers={"User-Agent": "Mozilla/5.0"},
            )
            if response.status_code == 200:
                data = response.json()
                translated = "".join(part[0] for part in data[0] if part and part[0])
                if translated and translated.lower() != text.lower():
                    return translated.strip().capitalize()
    except Exception:
        pass
    return text


async def _ai_enrich(word: str, part_of_speech: str | None) -> dict:
    try:
        prompt = f"""For the English word "{word}" ({part_of_speech or ""}), provide:
1. One short example sentence
2. 3-5 common collocations
3. 3-5 related words
Respond ONLY with valid JSON, no markdown:
{{"example": "...", "collocations": ["...", "..."], "related_words": ["...", "..."]}}"""

        async with httpx.AsyncClient(timeout=15.0) as client:
            response = await client.post(
                "https://api.groq.com/openai/v1/chat/completions",
                headers={
                    "Authorization": f"Bearer {settings.groq_api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": "llama-3.1-8b-instant",  # free, nhanh
                    "messages": [{"role": "user", "content": prompt}],
                    "max_tokens": 300,
                },
            )
            if response.status_code == 200:
                content = response.json()["choices"][0]["message"]["content"]
                content = content.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
                print(f"[ai_enrich] raw: {content}")  # thêm dòng này
                return json.loads(content)
            else:
                print(f"[ai_enrich] status={response.status_code} body={response.text}")
    except Exception as e:
        print(f"[ai_enrich ERROR] {word}: {e}")
    return {}


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
                (item.get("text") for item in phonetics if item.get("text")), None,
            )
            audio_url = next(
                (item.get("audio") for item in phonetics if item.get("audio")), None,
            )
            synonyms = _unique(
                (selected_meaning.get("synonyms") or []) + (definition.get("synonyms") or [])
            )
            antonyms = _unique(
                (selected_meaning.get("antonyms") or []) + (definition.get("antonyms") or [])
            )

            description_en = definition["definition"]
            example = definition.get("example")
            part_of_speech = selected_meaning.get("partOfSpeech")

            # Dịch từ gốc sang tiếng Việt
            meaning_vi = await _translate_vi(normalized_word)

            # Grok bổ sung example + collocations + related words
            ai_data = await _ai_enrich(normalized_word, part_of_speech)

            return DictionaryWord(
                word=entry.get("word") or normalized_word,
                phonetic=phonetic,
                audio_url=audio_url,
                part_of_speech=part_of_speech,
                meaning=meaning_vi,
                description_en=description_en,
                example=example or ai_data.get("example"),
                collocations=ai_data.get("collocations", []),
                related_words=ai_data.get("related_words", []),
                synonyms=synonyms[:5],
                antonyms=antonyms[:5],
            )
        except (IndexError, KeyError, StopIteration, TypeError, ValueError) as exc:
            raise api_error(502, "EXTERNAL_DICTIONARY_ERROR", "Dictionary returned an invalid response") from exc


dictionary_client = DictionaryClient()