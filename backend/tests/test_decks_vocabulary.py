from fastapi import HTTPException

from vocabulary.schemas import DictionaryWord


def test_deck_crud_and_ownership(client, auth_headers, create_deck, register_user):
    deck = create_deck(auth_headers)
    assert client.get("/decks", headers=auth_headers).json()[0]["name"] == "IELTS Basics"

    updated = client.patch(f"/decks/{deck['id']}", headers=auth_headers, json={"name": "Updated"})
    assert updated.status_code == 200
    assert updated.json()["name"] == "Updated"

    other = register_user("other@example.com")
    other_headers = {"Authorization": f"Bearer {other['access_token']}"}
    assert client.get(f"/decks/{deck['id']}", headers=other_headers).status_code == 404
    assert client.delete(f"/decks/{deck['id']}", headers=other_headers).status_code == 404
    assert client.delete(f"/decks/{deck['id']}", headers=auth_headers).status_code == 204


def test_vocabulary_crud_ownership_and_deck_delete(client, auth_headers, create_deck, create_word, register_user):
    deck = create_deck(auth_headers)
    word = create_word(auth_headers, deck["id"])
    listed = client.get(f"/decks/{deck['id']}/words", headers=auth_headers)
    assert listed.status_code == 200
    assert listed.json()[0]["word"] == "resilient"

    patched = client.patch(f"/words/{word['id']}", headers=auth_headers, json={"note": "Review this"})
    assert patched.json()["note"] == "Review this"

    other = register_user("other@example.com")
    other_headers = {"Authorization": f"Bearer {other['access_token']}"}
    assert client.patch(f"/words/{word['id']}", headers=other_headers, json={"note": "No"}).status_code == 404
    assert client.delete(f"/words/{word['id']}", headers=other_headers).status_code == 404

    assert client.delete(f"/decks/{deck['id']}", headers=auth_headers).status_code == 204
    assert client.get(f"/decks/{deck['id']}/words", headers=auth_headers).status_code == 404


def test_lookup_and_enriched_creation(client, auth_headers, create_deck, monkeypatch):
    async def fake_lookup(word: str) -> DictionaryWord:
        return DictionaryWord(
            word=word,
            phonetic="/rɪˈzɪliənt/",
            audio_url="https://example.com/resilient.mp3",
            part_of_speech="adjective",
            meaning="Able to recover quickly after difficulty.",
            example="She is resilient after many challenges.",
            synonyms=["strong"],
        )

    monkeypatch.setattr("vocabulary.router.dictionary_client.lookup_word", fake_lookup)
    monkeypatch.setattr("vocabulary.service.dictionary_client.lookup_word", fake_lookup)
    looked_up = client.post("/words/lookup", headers=auth_headers, json={"word": "resilient"})
    assert looked_up.status_code == 200
    assert looked_up.json()["part_of_speech"] == "adjective"

    deck = create_deck(auth_headers)
    enriched = client.post(f"/decks/{deck['id']}/words/enrich", headers=auth_headers, json={"word": "resilient"})
    assert enriched.status_code == 201
    assert enriched.json()["source"] == "dictionaryapi.dev"
    assert enriched.json()["meaning"].startswith("Able to recover")


def test_enrich_falls_back_to_manual_meaning(client, auth_headers, create_deck, monkeypatch):
    async def unavailable(_: str):
        raise HTTPException(status_code=502, detail={"code": "EXTERNAL_DICTIONARY_ERROR", "message": "Unavailable"})

    monkeypatch.setattr("vocabulary.service.dictionary_client.lookup_word", unavailable)
    deck = create_deck(auth_headers)
    manual = client.post(
        f"/decks/{deck['id']}/words/enrich",
        headers=auth_headers,
        json={"word": "custom", "meaning": "A manually entered meaning."},
    )
    assert manual.status_code == 201
    assert manual.json()["source"] == "manual"

    missing_meaning = client.post(f"/decks/{deck['id']}/words/enrich", headers=auth_headers, json={"word": "custom"})
    assert missing_meaning.status_code == 400
    assert missing_meaning.json()["detail"]["code"] == "VALIDATION_ERROR"


def test_csv_import_and_export(client, auth_headers, create_deck):
    deck = create_deck(auth_headers)
    csv_content = (
        "word,pronunciation,meaning,description_en,example,collocations,related_words,note\n"
        "focus,,Pay close attention,,,deep focus;focus group,concentrate,\n"
        "invalid,,,,,,,\n"
    )
    imported = client.post(
        f"/decks/{deck['id']}/import",
        headers=auth_headers,
        files={"file": ("words.csv", csv_content, "text/csv")},
    )
    assert imported.status_code == 200
    assert imported.json()["imported"] == 1
    assert imported.json()["skipped"] == 1

    exported = client.get(f"/decks/{deck['id']}/export", headers=auth_headers)
    assert exported.status_code == 200
    assert "focus" in exported.text
    assert "deep focus;focus group" in exported.text
