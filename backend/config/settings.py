from functools import lru_cache
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = "MinLish API"
    app_version: str = "0.1.0"
    environment: str = "local"

    database_url: str = Field(
        default="postgresql+psycopg://minlish:minlish@localhost:5432/minlish",
        alias="DATABASE_URL",
    )
    jwt_secret_key: str = Field(default="change-me", alias="JWT_SECRET_KEY")
    jwt_access_token_expire_minutes: int = Field(default=30, alias="JWT_ACCESS_TOKEN_EXPIRE_MINUTES")
    jwt_refresh_token_expire_days: int = Field(default=30, alias="JWT_REFRESH_TOKEN_EXPIRE_DAYS")

    dictionary_api_base_url: str = Field(
        default="https://api.dictionaryapi.dev/api/v2",
        alias="DICTIONARY_API_BASE_URL",
    )
    dictionary_api_timeout_seconds: float = Field(default=5.0, alias="DICTIONARY_API_TIMEOUT_SECONDS")

    groq_api_key: str = Field(default="", alias="GROQ_API_KEY")


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()