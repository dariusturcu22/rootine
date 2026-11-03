package app.aiclient;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CropResponse(
    String crop,
    @JsonProperty("diagnosis_supported") boolean diagnosisSupported,
    @JsonProperty("weather_supported") boolean weatherSupported,
    @JsonProperty("support_level") String supportLevel,
    @JsonProperty("weather_rules") List<String> weatherRules,
    List<String> classes) {}
