from http import HTTPStatus


def test_health_returns_ok_model_version_and_mock_flag(client):
    response = client.get("/health")

    assert response.status_code == HTTPStatus.OK
    assert response.json() == {"status": "ok", "model_version": "plant-v1", "mock": True}


def test_health_reports_real_mode_when_classifier_is_loaded(classifier_client):
    response = classifier_client.get("/health")

    assert response.status_code == HTTPStatus.OK
    assert response.json()["mock"] is False
