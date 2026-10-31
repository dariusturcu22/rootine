def test_health_returns_ok_and_model_version(client):
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "model_version": "plant-v1"}
