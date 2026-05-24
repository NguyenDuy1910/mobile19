# MinLish Backend

FastAPI backend for MinLish.

## Local Setup

```bash
cd backend
uv sync
cp .env.example .env
uv run uvicorn main:app --reload
```

After database migrations are added, run:

```bash
uv run alembic upgrade head
```

## Health Check

```bash
curl http://127.0.0.1:8000/health
```
