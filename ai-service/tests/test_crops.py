def test_crops_lists_enabled_crops_with_classes(client):
    response = client.get("/v1/crops")

    assert response.status_code == 200
    crops = {c["crop"]: c for c in response.json()}

    assert set(crops) == {"potato", "tomato"}

    assert len(crops["tomato"]["classes"]) == 10
    assert len(crops["potato"]["classes"]) == 3

    assert crops["tomato"]["diagnosis_supported"] is True
    assert crops["tomato"]["weather_supported"] is True
    assert crops["tomato"]["support_level"] == "lab_only"
    assert crops["tomato"]["weather_rules"] == ["hutton", "tomcast"]

    assert crops["potato"]["diagnosis_supported"] is True
    assert crops["potato"]["weather_supported"] is True

    assert set(crops["tomato"]["classes"]) == {
        "bacterial_spot",
        "early_blight",
        "late_blight",
        "leaf_mold",
        "septoria_leaf_spot",
        "spider_mites two-spotted_spider_mite",
        "target_spot",
        "tomato_yellow_leaf_curl_virus",
        "tomato_mosaic_virus",
        "healthy",
    }

    assert set(crops["potato"]["classes"]) == {
        "early_blight",
        "late_blight",
        "healthy",
    }
