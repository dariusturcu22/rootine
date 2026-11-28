package app.risk.service;

import app.aiclient.AiServiceClient;
import app.aiclient.AiServiceException;
import app.aiclient.RiskRequest;
import app.alert.AlertMessage;
import app.alert.AlertPublisher;
import app.parcel.domain.Parcel;
import app.parcel.repository.ParcelRepository;
import app.risk.domain.ParcelRisk;
import app.risk.domain.StoredDiseaseRisk;
import app.risk.dto.RiskLevel;
import app.risk.repository.ParcelRiskRepository;
import java.util.List;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Refreshes the stored weather risk for every parcel across all users. For each parcel it calls the
 * AI service with the parcel centre and crop, then stores the returned result. Parcels are
 * processed independently: a failed AI request is logged and skipped, so it neither stops later
 * parcels nor replaces that parcel's last good result. Weather-rule calculations stay in the AI
 * service; this class only stores what the AI returns.
 */
@Service
public class RiskRefreshService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RiskRefreshService.class);

  private final ParcelRepository parcelRepository;
  private final ParcelRiskRepository parcelRiskRepository;
  private final AiServiceClient aiServiceClient;
  private final AlertPublisher alertPublisher;
  private final TransactionTemplate readOnlyTransaction;
  private final TransactionTemplate writeTransaction;

  public RiskRefreshService(
      ParcelRepository parcelRepository,
      ParcelRiskRepository parcelRiskRepository,
      AiServiceClient aiServiceClient,
      AlertPublisher alertPublisher,
      PlatformTransactionManager transactionManager) {
    this.parcelRepository = parcelRepository;
    this.parcelRiskRepository = parcelRiskRepository;
    this.aiServiceClient = aiServiceClient;
    this.alertPublisher = alertPublisher;
    this.readOnlyTransaction = new TransactionTemplate(transactionManager);
    this.readOnlyTransaction.setReadOnly(true);
    this.writeTransaction = new TransactionTemplate(transactionManager);
  }

  /**
   * Refreshes the stored risk for every parcel and returns how many were stored successfully. The
   * AI calls run outside any transaction; each successful result is saved in its own transaction.
   */
  public int refreshAllParcels() {
    List<ParcelSnapshot> parcels =
        readOnlyTransaction.execute(
            status ->
                parcelRepository.findAll().stream().map(RiskRefreshService::toSnapshot).toList());

    int stored = 0;
    for (ParcelSnapshot parcel : parcels) {
      if (refreshOne(parcel)) {
        stored++;
      }
    }
    LOGGER.info("Risk refresh finished: stored {} of {} parcels", stored, parcels.size());
    return stored;
  }

  private boolean refreshOne(ParcelSnapshot parcel) {
    app.aiclient.RiskResponse aiResponse;
    try {
      aiResponse =
          aiServiceClient.getRisk(
              new RiskRequest(parcel.latitude(), parcel.longitude(), parcel.crop(), null));
    } catch (AiServiceException exception) {
      // Keep the last good result; log and move on to the next parcel.
      LOGGER.warn(
          "Risk refresh skipped parcel {} (crop {}): {}",
          parcel.id(),
          parcel.crop(),
          exception.getMessage());
      return false;
    }

    try {
      writeTransaction.executeWithoutResult(status -> store(parcel.id(), aiResponse));
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "Risk refresh could not store parcel {}: {}", parcel.id(), exception.getMessage());
      return false;
    }
    publishHighRiskAlert(parcel, aiResponse);
    return true;
  }

  private void publishHighRiskAlert(ParcelSnapshot parcel, app.aiclient.RiskResponse aiResponse) {
    if (!"high".equals(aiResponse.level())) {
      return;
    }
    AlertMessage message =
        new AlertMessage(
            parcel.userId(),
            parcel.recipientEmail(),
            parcel.id(),
            parcel.name(),
            parcel.crop(),
            RiskLevel.HIGH,
            aiResponse.riskIndex(),
            aiResponse.reason(),
            aiResponse.generatedAt());
    try {
      alertPublisher.publish(message);
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "Could not publish high-risk alert for parcel {}: {}",
          parcel.id(),
          exception.getMessage(),
          exception);
    }
  }

  private void store(UUID parcelId, app.aiclient.RiskResponse aiResponse) {
    Parcel parcel = parcelRepository.getReferenceById(parcelId);
    List<StoredDiseaseRisk> diseases =
        aiResponse.diseases() == null
            ? List.of()
            : aiResponse.diseases().stream()
                .map(
                    disease ->
                        new StoredDiseaseRisk(
                            disease.disease(),
                            disease.model(),
                            disease.level(),
                            disease.riskIndex()))
                .toList();
    ParcelRisk existing = parcelRiskRepository.findById(parcelId).orElse(null);
    if (existing == null) {
      parcelRiskRepository.save(
          new ParcelRisk(
              parcel,
              aiResponse.level(),
              aiResponse.riskIndex(),
              aiResponse.reason(),
              diseases,
              aiResponse.generatedAt()));
    } else {
      existing.setLevel(aiResponse.level());
      existing.setRiskIndex(aiResponse.riskIndex());
      existing.setReason(aiResponse.reason());
      existing.setDiseases(diseases);
      existing.setGeneratedAt(aiResponse.generatedAt());
      parcelRiskRepository.save(existing);
    }
  }

  private static ParcelSnapshot toSnapshot(Parcel parcel) {
    Coordinate centre = parcel.getGeometry().getCentroid().getCoordinate();
    return new ParcelSnapshot(
        parcel.getId(),
        parcel.getOwner().getId(),
        parcel.getOwner().getEmail(),
        parcel.getName(),
        parcel.getCrop(),
        centre.getY(),
        centre.getX());
  }

  /** Immutable parcel data read in the first transaction and used for the AI calls afterwards. */
  private record ParcelSnapshot(
      UUID id,
      UUID userId,
      String recipientEmail,
      String name,
      String crop,
      double latitude,
      double longitude) {}
}
