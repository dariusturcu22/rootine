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
