import os

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import event

os.environ["DATABASE_URL"] = "sqlite+pysqlite:///:memory:"
os.environ["JWT_SECRET_KEY"] = "test-secret"

from database.base import Base  # noqa: E402
import database.models  # noqa: E402, F401
from database.session import engine  # noqa: E402
from main import app  # noqa: E402


@event.listens_for(engine, "connect")
def _enable_sqlite_foreign_keys(dbapi_connection, _) -> None:
    cursor = dbapi_connection.cursor()
    cursor.execute("PRAGMA foreign_keys=ON")
    cursor.close()


@pytest.fixture(autouse=True)
def reset_database():
    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)
    yield
    Base.metadata.drop_all(bind=engine)


@pytest.fixture
def client() -> TestClient:
    return TestClient(app)


@pytest.fixture
def register_user(client: TestClient):
    def _register(email: str = "learner@example.com", password: str = "password123") -> dict:
        response = client.post("/auth/register", json={"email": email, "password": password})
        assert response.status_code == 201, response.text
        return response.json()

    return _register


@pytest.fixture
def auth_headers(register_user):
    tokens = register_user()
    return {"Authorization": f"Bearer {tokens['access_token']}"}


@pytest.fixture
def create_deck(client: TestClient):
    def _create(headers: dict[str, str], name: str = "IELTS Basics") -> dict:
        response = client.post("/decks", headers=headers, json={"name": name})
        assert response.status_code == 201, response.text
        return response.json()

    return _create


@pytest.fixture
def create_word(client: TestClient):
    def _create(headers: dict[str, str], deck_id: str, word: str = "resilient", meaning: str = "Able to recover quickly.") -> dict:
        response = client.post(
            f"/decks/{deck_id}/words",
            headers=headers,
            json={"word": word, "meaning": meaning},
        )
        assert response.status_code == 201, response.text
        return response.json()

    return _create
