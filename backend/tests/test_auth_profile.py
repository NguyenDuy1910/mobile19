def test_health_and_openapi(client):
    assert client.get("/health").json() == {"status": "ok"}
    assert client.get("/docs").status_code == 200
    assert client.get("/openapi.json").status_code == 200


def test_register_duplicate_login_refresh_and_current_user(client):
    payload = {"email": " Learner@Example.com ", "password": "password123"}
    registered = client.post("/auth/register", json=payload)
    assert registered.status_code == 201
    tokens = registered.json()
    assert tokens["user"]["email"] == "learner@example.com"
    assert "password_hash" not in tokens["user"]

    duplicate = client.post("/auth/register", json={"email": "learner@example.com", "password": "password123"})
    assert duplicate.status_code == 409
    assert duplicate.json()["detail"]["code"] == "DUPLICATE_EMAIL"

    invalid = client.post("/auth/login", json={"email": "learner@example.com", "password": "wrong"})
    assert invalid.status_code == 401
    assert invalid.json()["detail"]["code"] == "INVALID_CREDENTIALS"

    logged_in = client.post("/auth/login", json={"email": "learner@example.com", "password": "password123"})
    assert logged_in.status_code == 200
    headers = {"Authorization": f"Bearer {logged_in.json()['access_token']}"}
    assert client.get("/me", headers=headers).json()["email"] == "learner@example.com"

    refreshed = client.post("/auth/refresh", json={"refresh_token": tokens["refresh_token"]})
    assert refreshed.status_code == 200
    assert refreshed.json()["access_token"] != tokens["access_token"]
    assert client.post("/auth/logout", headers=headers).status_code == 200


def test_current_user_requires_access_token(client):
    response = client.get("/me")
    assert response.status_code == 401
    assert response.json()["detail"]["code"] == "UNAUTHORIZED"


def test_profile_update(client, auth_headers):
    response = client.patch(
        "/me/profile",
        headers=auth_headers,
        json={
            "name": "Min",
            "learning_goal": "IELTS",
            "english_level": "B1",
            "daily_new_words": 12,
            "daily_review_limit": 40,
            "notification_time": "20:30:00",
            "timezone": "Asia/Ho_Chi_Minh",
        },
    )
    assert response.status_code == 200
    assert response.json()["learning_goal"] == "IELTS"
    assert response.json()["daily_new_words"] == 12
