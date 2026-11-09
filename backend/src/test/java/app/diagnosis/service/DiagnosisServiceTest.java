package app.diagnosis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.aiclient.AiServiceClient;
import app.aiclient.Prediction;
import app.aiclient.PredictionResponse;
import app.diagnosis.domain.Diagnosis;
import app.diagnosis.dto.DiagnosisResponse;
import app.diagnosis.mapper.DiagnosisMapper;
import app.diagnosis.repository.DiagnosisRepository;
import app.parcel.domain.Parcel;
import app.parcel.repository.ParcelRepository;
import app.user.domain.UserAccount;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Unit tests for the diagnosis orchestration. They confirm the AI network call runs while no
 * database transaction is active (BE-11). A stub {@link PlatformTransactionManager} stands in for
 * the real one, so no database or container is needed; it still runs the transactional callbacks.
 */
class DiagnosisServiceTest {
  private static final Path UPLOAD_DIRECTORY = Path.of("target", "test-uploads-unit");

  private final AiServiceClient aiServiceClient = mock(AiServiceClient.class);
  private final DiagnosisMapper diagnosisMapper = mock(DiagnosisMapper.class);
  private final DiagnosisRepository diagnosisRepository = mock(DiagnosisRepository.class);
  private final ParcelRepository parcelRepository = mock(ParcelRepository.class);
  private final PlatformTransactionManager transactionManager =
      mock(PlatformTransactionManager.class);

  private final DiagnosisService diagnosisService =
      new DiagnosisService(
          aiServiceClient,
          diagnosisMapper,
          diagnosisRepository,
          parcelRepository,
          UPLOAD_DIRECTORY,
          transactionManager);

  @AfterEach
  void cleanUploadDirectory() throws IOException {
    if (Files.exists(UPLOAD_DIRECTORY)) {
      try (var files = Files.walk(UPLOAD_DIRECTORY)) {
        files.sorted(Comparator.reverseOrder()).forEach(DiagnosisServiceTest::deleteQuietly);
      }
    }
  }

  private static void deleteQuietly(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException ignored) {
      // Best-effort cleanup of test artifacts.
    }
  }

  @Test
  void callsAiServiceWithoutAnOpenTransaction() {
    // The stub manager returns a status and does nothing on commit, so the callbacks run but no
    // real transaction is bound to the thread.
    when(transactionManager.getTransaction(ArgumentMatchers.any()))
        .thenReturn(new SimpleTransactionStatus());

    UUID ownerId = UUID.randomUUID();
    UUID parcelId = UUID.randomUUID();
    Parcel parcel =
        new Parcel(
            parcelId,
            new UserAccount(ownerId, "owner@example.com", "hash", Instant.now()),
            "North field",
            "potato",
            null,
            Instant.now());
    when(parcelRepository.findByIdAndOwnerId(parcelId, ownerId)).thenReturn(Optional.of(parcel));

    AtomicBoolean transactionActiveDuringAiCall = new AtomicBoolean(true);
    when(aiServiceClient.predict(any(), eq("potato")))
        .thenAnswer(
            invocation -> {
              transactionActiveDuringAiCall.set(
                  TransactionSynchronizationManager.isActualTransactionActive());
              return new PredictionResponse(
                  "confident",
                  "potato",
                  "ok",
                  List.of(new Prediction("potato_early_blight", "early_blight", 0.92)),
                  "lab_only",
                  "plant-v1");
            });
    when(diagnosisRepository.save(any(Diagnosis.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    DiagnosisResponse mappedResponse =
        new DiagnosisResponse(
            UUID.randomUUID(),
            parcelId,
            app.diagnosis.dto.DiagnosisStatus.CONFIDENT,
            "potato",
            null,
            List.of(),
            null,
            "plant-v1",
            Instant.now());
    when(diagnosisMapper.toResponse(any(Diagnosis.class))).thenReturn(mappedResponse);

    MockMultipartFile image =
        new MockMultipartFile(
            "image",
            "leaf.jpg",
            "image/jpeg",
            new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01, 0x02});

    DiagnosisResponse response = diagnosisService.createDiagnosis(ownerId, image, parcelId);

    assertThat(response.parcelId()).isEqualTo(parcelId);
    assertThat(transactionActiveDuringAiCall)
        .as("the AI call must run with no database transaction open")
        .isFalse();
  }
}
