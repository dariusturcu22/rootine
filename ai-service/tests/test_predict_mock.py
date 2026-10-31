def post_image(client, content, crop=None):
    data = {"crop": crop} if crop else None
    return client.post(
        "/v1/predict", files={"image": ("leaf.png", content, "image/png")}, data=data
    )


def test_predict_without_crop_returns_three_predictions(client, png_bytes):
    body = post_image(client, png_bytes).json()

    assert body["status"] == "confident"
    assert body["crop_check"] == "ok"
    assert body["model_version"] == "plant-v1"
    assert len(body["top_predictions"]) == 3
    assert set(body["top_predictions"][0]) == {"label", "disease", "probability"}


def test_predict_with_crop_uses_that_crop(client, png_bytes):
    body = post_image(client, png_bytes, crop="potato").json()

    assert body["status"] == "confident"
    assert body["crop"] == "potato"
    assert body["top_predictions"][0]["label"] == "potato_early_blight"


def test_predict_mock_labels_exist_in_catalog(client, png_bytes):
    classes = {c["crop"]: c["classes"] for c in client.get("/v1/crops").json()}
    body = post_image(client, png_bytes, crop="tomato").json()

    assert {p["disease"] for p in body["top_predictions"]} <= set(classes["tomato"])


def test_predict_unknown_crop_is_not_supported(client, png_bytes):
    body = post_image(client, png_bytes, crop="banana").json()

    assert body["status"] == "crop_not_supported"
    assert body["support_level"] == "not_supported"
    assert body["top_predictions"] == []


def test_predict_garbage_bytes_is_invalid_image(client):
    body = post_image(client, b"not an image").json()

    assert body["status"] == "invalid_image"
    assert body["top_predictions"] == []
