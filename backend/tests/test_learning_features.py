from vocabulary.schemas import DictionaryWord


def test_learning_review_due_words_progress_and_sessions(client, auth_headers, create_deck, create_word):
    deck = create_deck(auth_headers)
    word = create_word(auth_headers, deck["id"])

    due = client.get("/learning/due-words", headers=auth_headers)
    assert due.status_code == 200
    assert due.json()[0]["vocab_item"]["id"] == word["id"]
    plan = client.get("/learning/daily-plan", headers=auth_headers).json()
    assert plan["new_words_due"] == 1

    reviewed = client.post(
        "/learning/review",
        headers=auth_headers,
        json={"vocab_item_id": word["id"], "rating": "good"},
    )
    assert reviewed.status_code == 200
    assert reviewed.json()["interval_days"] == 1
    assert client.get("/learning/due-words", headers=auth_headers).json() == []

    summary = client.get("/progress/summary", headers=auth_headers).json()
    assert summary["learned_words"] == 1
    assert summary["accuracy_percentage"] == 100.0
    assert client.get("/progress/retention", headers=auth_headers).json()["total_reviews"] == 1
    assert client.get("/progress/daily-activity", headers=auth_headers).json()[0]["reviews"] == 1
    assert client.get(f"/progress/decks/{deck['id']}", headers=auth_headers).json()["learned_words"] == 1

    session = client.post("/learning/session/start", headers=auth_headers).json()
    ended = client.post(
        "/learning/session/end",
        headers=auth_headers,
        json={"session_id": session["id"], "new_words_count": 1, "review_words_count": 1, "correct_count": 1},
    )
    assert ended.status_code == 200
    assert ended.json()["duration_seconds"] >= 0


def test_learning_review_rejects_foreign_word(client, auth_headers, create_deck, create_word, register_user):
    other = register_user("other@example.com")
    other_headers = {"Authorization": f"Bearer {other['access_token']}"}
    deck = create_deck(other_headers, "Private")
    word = create_word(other_headers, deck["id"], "private", "Only mine")
    response = client.post(
        "/learning/review",
        headers=auth_headers,
        json={"vocab_item_id": word["id"], "rating": "again"},
    )
    assert response.status_code == 404


def test_practice_notifications_and_agent(client, auth_headers, create_deck, create_word, monkeypatch):
    deck = create_deck(auth_headers)
    word = create_word(auth_headers, deck["id"], "resilient", "Able to recover quickly.")
    create_word(auth_headers, deck["id"], "focus", "Pay close attention.")

    questions = client.post("/practice/generate", headers=auth_headers, json={"limit": 2})
    assert questions.status_code == 200
    assert len(questions.json()) == 2
    submitted = client.post(
        "/practice/submit",
        headers=auth_headers,
        json={"vocab_item_id": word["id"], "user_answer": "Able to recover quickly."},
    )
    assert submitted.status_code == 201
    assert submitted.json()["is_correct"] is True
    assert len(client.get("/practice/history", headers=auth_headers).json()) == 1

    device = client.post(
        "/notifications/register-device",
        headers=auth_headers,
        json={"platform": "android", "device_token": "device-123"},
    )
    assert device.status_code == 201
    settings = client.patch(
        "/notifications/settings",
        headers=auth_headers,
        json={"notifications_enabled": True, "notification_time": "20:00:00", "timezone": "Asia/Ho_Chi_Minh"},
    )
    assert settings.status_code == 200
    assert settings.json()["timezone"] == "Asia/Ho_Chi_Minh"
    assert client.post("/notifications/test", headers=auth_headers).status_code == 201

    async def fake_lookup(word_value: str) -> DictionaryWord:
        return DictionaryWord(
            word=word_value,
            meaning="Able to recover quickly after difficulty.",
            example="She is resilient after many challenges.",
        )

    monkeypatch.setattr("agent.service.dictionary_client.lookup_word", fake_lookup)
    explanation = client.post("/agent/explain-word", headers=auth_headers, json={"word": "resilient"})
    assert explanation.status_code == 200
    assert explanation.json()["simple_explanation"].startswith("Able to recover")
    assert explanation.json()["collocations"][0] == "resilient person"
    assert client.post("/agent/generate-examples", headers=auth_headers, json={"word": "resilient"}).status_code == 200
    assert client.post("/agent/generate-deck", headers=auth_headers, json={"topic": "IELTS"}).status_code == 200
