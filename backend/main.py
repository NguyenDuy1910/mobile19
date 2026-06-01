from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from agent.router import router as agent_router
from auth.router import router as auth_router
from config.settings import settings
from decks.router import router as decks_router
from learning.router import router as learning_router
from notifications.router import router as notifications_router
from practice.router import router as practice_router
from progress.router import router as progress_router
from users.router import router as users_router
from vocabulary.router import router as vocabulary_router

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
)

app.include_router(auth_router)
app.include_router(users_router)
app.include_router(decks_router)
app.include_router(vocabulary_router)
app.include_router(learning_router)
app.include_router(progress_router)
app.include_router(practice_router)
app.include_router(notifications_router)
app.include_router(agent_router)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(_: Request, exc: RequestValidationError) -> JSONResponse:
    return JSONResponse(
        status_code=422,
        content={
            "detail": {
                "code": "VALIDATION_ERROR",
                "message": "Request validation failed",
                "errors": exc.errors(),
            }
        },
    )


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "ok"}
