def test_risk_for_modelled_crop(client):
    response = client.post(
        "/v1/risk", json={"latitude": 46.77, "longitude": 23.59, "crop": "potato"}
    )

    body = response.json()
    assert response.status_code == 200
    assert body["level"] == "medium"
    assert 0 <= body["risk_index"] <= 100
    assert body["diseases"][0]["model"] == "hutton"
    assert "generated_at" in body


def test_risk_for_unknown_crop_is_not_modelled(client):
    body = client.post(
        "/v1/risk", json={"latitude": 46.77, "longitude": 23.59, "crop": "banana"}
    ).json()

    assert body["level"] == "not_modelled"
    assert body["diseases"] == []


def test_risk_rejects_invalid_latitude(client):
    response = client.post("/v1/risk", json={"latitude": 123, "longitude": 23.59, "crop": "potato"})

    assert response.status_code == 422


def test_threat_mock_returns_level_and_reason(client):
    body = client.post("/v1/threat").json()

    assert set(body) == {"level", "reason"}
