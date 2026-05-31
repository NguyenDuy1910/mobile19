# MinLish Backend

FastAPI MVP backend for MinLish vocabulary learning.

## Local Setup

From the repository root:

```bash
docker compose up -d postgres

cd backend
uv sync
cp .env.example .env
uv run alembic upgrade head
uv run uvicorn main:app --reload
```

OpenAPI documentation is available at:

- `http://127.0.0.1:8000/docs`
- `http://127.0.0.1:8000/openapi.json`

## Tests

Tests use an isolated in-memory SQLite database, so Docker is not required:

```bash
cd backend
uv run pytest
uv run ruff check . --exclude .venv
```

## Main Endpoints

### Health and Auth

- `GET /health`
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /me`
- `PATCH /me/profile`

### Vocabulary Learning

- `GET|POST /decks`
- `GET|PATCH|DELETE /decks/{deck_id}`
- `GET|POST /decks/{deck_id}/words`
- `PATCH|DELETE /words/{word_id}`
- `POST /words/lookup`
- `POST /decks/{deck_id}/words/enrich`
- `POST /decks/{deck_id}/import`
- `GET /decks/{deck_id}/export`
- `GET /learning/daily-plan`
- `GET /learning/due-words`
- `POST /learning/review`
- `POST /learning/session/start`
- `POST /learning/session/end`

### Progress, Practice, Notifications, and Agent

- `GET /progress/summary`
- `GET /progress/daily-activity`
- `GET /progress/retention`
- `GET /progress/decks/{deck_id}`
- `POST /practice/generate`
- `POST /practice/submit`
- `GET /practice/history`
- `POST /notifications/register-device`
- `PATCH /notifications/settings`
- `POST /notifications/test`
- `POST /agent/explain-word`
- `POST /agent/generate-examples`
- `POST /agent/generate-deck`

## MVP Notes

- DictionaryAPI.dev lookups are normalized before storage. Viewing a saved vocabulary item does not call the external API again.
- Manual enriched-word creation still works when DictionaryAPI.dev is unavailable if a meaning is provided.
- Logout is stateless for the MVP: clients discard tokens. A server-side token revocation store can be added later.
- Notification test calls store a simulated notification record. No push provider is configured yet.
- Agent endpoints return deterministic structured learning output. They do not require paid API keys.
