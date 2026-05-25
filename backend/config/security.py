from datetime import timedelta

from config.settings import settings


ACCESS_TOKEN_EXPIRE_DELTA = timedelta(minutes=settings.jwt_access_token_expire_minutes)
REFRESH_TOKEN_EXPIRE_DELTA = timedelta(days=settings.jwt_refresh_token_expire_days)
