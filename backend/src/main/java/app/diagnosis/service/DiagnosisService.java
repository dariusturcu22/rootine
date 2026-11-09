package app.diagnosis.service;

import app.aiclient.AiServiceClient;
import app.aiclient.AiServiceException;
import app.aiclient.PredictionResponse;
import app.common.SupportLevel;
import app.diagnosis.domain.Diagnosis;
import app.diagnosis.domain.DiagnosisResult;
import app.diagnosis.dto.CropCheck;
import app.diagnosis.dto.DiagnosisResponse;
import app.diagnosis.dto.DiagnosisStatus;
import app.diagnosis.dto.Prediction;
import app.diagnosis.mapper.DiagnosisMapper;
import app.diagnosis.repository.DiagnosisRepository;
import app.parcel.domain.Parcel;
import app.parcel.repository.ParcelRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** Coordinates diagnosis uploads, AI predictions, and owner-scoped diagnosis reads. */
@Service
public class DiagnosisService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosisService.class);
  private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
  private static final byte[] PNG_SIGNATURE =
      new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

  private final AiServiceClient aiServiceClient;
  private final DiagnosisMapper diagnosisMapper;
  private final DiagnosisRepository diagnosisRepository;
  private final ParcelRepository parcelRepository;
  private final Path uploadDirectory;
  private final TransactionTemplate readOnlyTransaction;
  private final TransactionTemplate writeTransaction;

  public DiagnosisService(
      AiServiceClient aiServiceClient,
      DiagnosisMapper diagnosisMapper,
      DiagnosisRepository diagnosisRepository,
      ParcelRepository parcelRepository,
      @Value("${app.upload-dir}") Path uploadDirectory,
      PlatformTransactionManager transactionManager) {
    this.aiServiceClient = aiServiceClient;
    this.diagnosisMapper = diagnosisMapper;
    this.diagnosisRepository = diagnosisRepository;
    this.parcelRepository = parcelRepository;
    this.uploadDirectory = uploadDirectory;
    this.readOnlyTransaction = new TransactionTemplate(transactionManager);
    this.readOnlyTransaction.setReadOnly(true);
    this.writeTransaction = new TransactionTemplate(transactionManager);
  }

  /**
   * Creates a diagnosis. The database work is wrapped in two short transactions (load the parcel,
   * then save the diagnosis); the slow AI service call and the photo write happen between them with
   * no transaction open, so a network call never holds a database connection and transaction.
   */
  public DiagnosisResponse createDiagnosis(UUID ownerId, MultipartFile image, UUID parcelId) {
    if (image == null || parcelId == null) {
      throw new InvalidDiagnosisUploadException("Both image and parcelId are required.");
    }

    if (image.getSize() > MAX_IMAGE_SIZE) {
      throw new DiagnosisFileTooLargeException("The image must not be larger than 10 MB.");
    }

    byte[] imageBytes = readAndValidateImage(image);

    // Transaction 1 (read-only): load and ownership-check the parcel, then commit.
    Parcel parcel =
        readOnlyTransaction.execute(
            status ->
                parcelRepository
                    .findByIdAndOwnerId(parcelId, ownerId)
                    .orElseThrow(
                        () -> new DiagnosisNotFoundException("The parcel was not found.")));
    String crop = parcel.getCrop();

    // No transaction open: call the AI service and write the photo to disk.
    PredictionResponse prediction;
    try {
      prediction = aiServiceClient.predict(image, crop);
    } catch (AiServiceException exception) {
      LOGGER.warn(
          "AI prediction failed for parcel {} (crop {}): {}",
          parcelId,
          crop,
          exception.getMessage());
      throw exception;
    }

    DiagnosisResult result = toDiagnosisResult(prediction);

    String extension = hasPngSignature(imageBytes) ? ".png" : ".jpg";
    Path photoPath = uploadDirectory.resolve(UUID.randomUUID() + extension);
    try {
      Files.createDirectories(uploadDirectory);
      Files.write(photoPath, imageBytes);
    } catch (IOException exception) {
      throw new UncheckedIOException("Could not save the uploaded diagnosis photo.", exception);
    }

    Diagnosis diagnosis =
        new Diagnosis(
            UUID.randomUUID(),
            parcel,
            photoPath.toString(),
            result,
            prediction.modelVersion(),
            Instant.now());

    // Transaction 2: persist the finished diagnosis.
    try {
      DiagnosisResponse response =
          writeTransaction.execute(
              status -> diagnosisMapper.toResponse(diagnosisRepository.save(diagnosis)));
      LOGGER.info(
          "Saved diagnosis {} for parcel {} with status {}",
          response.id(),
          parcelId,
          result.status());
      return response;
    } catch (RuntimeException exception) {
      try {
        Files.deleteIfExists(photoPath);
      } catch (IOException cleanupException) {
        exception.addSuppressed(cleanupException);
      }
      throw exception;
    }
  }

  @Transactional(readOnly = true)
  public List<DiagnosisResponse> listDiagnoses(UUID ownerId, UUID parcelId) {
    List<Diagnosis> diagnoses;
    if (parcelId == null) {
      diagnoses = diagnosisRepository.findByParcelOwnerIdOrderByCreatedAtDesc(ownerId);
    } else {
      if (!parcelRepository.existsByIdAndOwnerId(parcelId, ownerId)) {
        throw new DiagnosisNotFoundException("The parcel was not found.");
      }
      diagnoses =
          diagnosisRepository.findByParcelIdAndParcelOwnerIdOrderByCreatedAtDesc(parcelId, ownerId);
    }
    return diagnoses.stream().map(diagnosisMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public DiagnosisResponse getDiagnosis(UUID ownerId, UUID diagnosisId) {
    Diagnosis diagnosis =
        diagnosisRepository
            .findByIdAndParcelOwnerId(diagnosisId, ownerId)
            .orElseThrow(() -> new DiagnosisNotFoundException("The diagnosis was not found."));
    return diagnosisMapper.toResponse(diagnosis);
  }

  private static byte[] readAndValidateImage(MultipartFile image) {
    try {
      byte[] bytes = image.getBytes();
      if (!hasJpegSignature(bytes) && !hasPngSignature(bytes)) {
        throw new InvalidDiagnosisUploadException("The image must be a JPEG or PNG file.");
      }
      return bytes;
    } catch (IOException exception) {
      throw new InvalidDiagnosisUploadException("The uploaded image could not be read.", exception);
    }
  }

  private static boolean hasJpegSignature(byte[] bytes) {
    return bytes.length >= 3
        && (bytes[0] & 0xff) == 0xff
        && (bytes[1] & 0xff) == 0xd8
        && (bytes[2] & 0xff) == 0xff;
  }

  private static boolean hasPngSignature(byte[] bytes) {
    if (bytes.length < PNG_SIGNATURE.length) {
      return false;
    }
    for (int index = 0; index < PNG_SIGNATURE.length; index++) {
      if (bytes[index] != PNG_SIGNATURE[index]) {
        return false;
      }
    }
    return true;
  }

  private static DiagnosisResult toDiagnosisResult(PredictionResponse prediction) {
    if (prediction == null
        || prediction.status() == null
        || prediction.supportLevel() == null
        || prediction.modelVersion() == null) {
      throw new AiServiceException("AI service returned an incomplete prediction.");
    }
    try {
      DiagnosisStatus status = parseEnum(DiagnosisStatus.class, prediction.status());
      CropCheck cropCheck =
          prediction.cropCheck() == null
              ? null
              : parseEnum(CropCheck.class, prediction.cropCheck());
      SupportLevel supportLevel = parseEnum(SupportLevel.class, prediction.supportLevel());
      List<Prediction> topPredictions =
          prediction.topPredictions() == null
              ? List.of()
              : prediction.topPredictions().stream()
                  .map(item -> new Prediction(item.label(), item.disease(), item.probability()))
                  .toList();
      return new DiagnosisResult(
          status, prediction.crop(), cropCheck, topPredictions, supportLevel);
    } catch (IllegalArgumentException exception) {
      throw new AiServiceException("AI service returned an invalid prediction.", exception);
    }
  }

  private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
    return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
  }
}
