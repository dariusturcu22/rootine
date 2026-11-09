package app.risk.service;

import app.aiclient.AiServiceClient;
import app.aiclient.AiServiceException;
import app.aiclient.RiskRequest;
import app.parcel.domain.Parcel;
import app.parcel.repository.ParcelRepository;
import app.parcel.service.ParcelNotFoundException;
import app.risk.dto.DiseaseRisk;
import app.risk.dto.RiskLevel;
import app.risk.dto.RiskResponse;
import java.util.List;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads an owned parcel and obtains its current weather risk from the AI service. */
@Service
public class RiskService {

  private final ParcelRepository parcelRepository;
  private final AiServiceClient aiServiceClient;

  public RiskService(ParcelRepository parcelRepository, AiServiceClient aiServiceClient) {
    this.parcelRepository = parcelRepository;
    this.aiServiceClient = aiServiceClient;
  }

  @Transactional(readOnly = true)
  public RiskResponse getParcelRisk(UUID ownerId, UUID parcelId) {
    Parcel parcel =
        parcelRepository
            .findByIdAndOwnerId(parcelId, ownerId)
            .orElseThrow(ParcelNotFoundException::new);

    Coordinate centre = parcel.getGeometry().getCentroid().getCoordinate();
    app.aiclient.RiskResponse aiResponse =
        aiServiceClient.getRisk(
            new RiskRequest(centre.getY(), centre.getX(), parcel.getCrop(), null));
    return toResponse(parcel, aiResponse);
  }

  private static RiskResponse toResponse(Parcel parcel, app.aiclient.RiskResponse response) {
    return new RiskResponse(
        parcel.getId(),
        response.crop(),
        toRiskLevel(response.level()),
        response.riskIndex(),
        response.reason(),
        response.diseases() == null
            ? List.of()
            : response.diseases().stream()
                .map(
                    disease ->
                        new DiseaseRisk(
                            disease.disease(),
                            disease.model(),
                            toRiskLevel(disease.level()),
                            disease.riskIndex()))
                .toList(),
        response.generatedAt());
  }

  private static RiskLevel toRiskLevel(String value) {
    return java.util.Arrays.stream(RiskLevel.values())
        .filter(level -> level.jsonValue().equals(value))
        .findFirst()
        .orElseThrow(() -> new AiServiceException("AI service returned an unknown risk level."));
  }
}
