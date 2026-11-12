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
import java.util.Locale;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Service;

/** Loads an owned parcel and obtains its current weather risk from the AI service. */
@Service
public class RiskService {

  private final ParcelRepository parcelRepository;
  private final AiServiceClient aiServiceClient;

  public RiskService(ParcelRepository parcelRepository, AiServiceClient aiServiceClient) {
    this.parcelRepository = parcelRepository;
    this.aiServiceClient = aiServiceClient;
  }

  public RiskResponse getParcelRisk(UUID ownerId, UUID parcelId) {
    Parcel parcel =
        parcelRepository
            .findByIdAndOwnerId(parcelId, ownerId)
            .orElseThrow(ParcelNotFoundException::new);
    Coordinate center = parcel.getGeometry().getCentroid().getCoordinate();
    app.aiclient.RiskResponse aiResponse =
        aiServiceClient.getRisk(
            new RiskRequest(center.getY(), center.getX(), parcel.getCrop(), null));
    return toRiskResponse(parcel, aiResponse);
  }

  private static RiskResponse toRiskResponse(Parcel parcel, app.aiclient.RiskResponse response) {
    if (response == null
        || !parcel.getCrop().equals(response.crop())
        || response.level() == null
        || response.reason() == null
        || response.diseases() == null
        || response.generatedAt() == null) {
      throw new AiServiceException("AI service returned an incomplete risk result.");
    }

    try {
      List<DiseaseRisk> diseases =
          response.diseases().stream()
              .map(
                  disease ->
                      new DiseaseRisk(
                          disease.disease(),
                          disease.model(),
                          parseLevel(disease.level()),
                          disease.riskIndex()))
              .toList();
      return new RiskResponse(
          parcel.getId(),
          parcel.getCrop(),
          parseLevel(response.level()),
          response.riskIndex(),
          response.reason(),
          diseases,
          response.generatedAt());
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new AiServiceException("AI service returned an invalid risk result.", exception);
    }
  }

  private static RiskLevel parseLevel(String level) {
    try {
      return RiskLevel.valueOf(level.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new AiServiceException("AI service returned an unknown risk level.", exception);
    }
  }
}
