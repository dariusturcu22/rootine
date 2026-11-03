package app.aiclient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RiskRequest(
    double latitude,
    double longitude,
    String crop,
    @JsonProperty("replay_date") LocalDate replayDate) {}
