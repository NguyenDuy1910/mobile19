from fastapi import FastAPI

from config.settings import settings
from shared.datetime import utc_now

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
)


@app.get("/health")
def health_check() -> dict[str, str]:
    return {
        "status": "ok",
        "service": settings.app_name,
        "environment": settings.environment,
        "time": utc_now().isoformat(),
    }
