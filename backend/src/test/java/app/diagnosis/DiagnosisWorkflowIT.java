package app.diagnosis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.AbstractIntegrationTest;
import app.aiclient.AiServiceClient;
import app.aiclient.AiServiceException;
import app.aiclient.Prediction;
import app.aiclient.PredictionResponse;
import app.common.error.ProblemDetailResponse;
import app.diagnosis.domain.Diagnosis;
import app.diagnosis.repository.DiagnosisRepository;
import app.parcel.SpatialReference;
import app.parcel.domain.Parcel;
import app.parcel.repository.ParcelRepository;
import app.user.domain.UserAccount;
import app.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class DiagnosisWorkflowIT extends AbstractIntegrationTest {

  private static final GeometryFactory GEOMETRY_FACTORY =
      new GeometryFactory(new PrecisionModel(), SpatialReference.WGS84_SRID);
  private static final Path UPLOAD_DIRECTORY = Path.of("target", "test-uploads");
  private static final String PROBLEM_JSON = ProblemDetailResponse.MEDIA_TYPE;

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private ParcelRepository parcelRepository;
  @Autowired private DiagnosisRepository diagnosisRepository;
  @MockitoBean private AiServiceClient aiServiceClient;

  private Path uploadedPhoto;

  @AfterEach
  void removeUploadedPhoto() throws Exception {
    if (uploadedPhoto != null) {
      Files.deleteIfExists(uploadedPhoto);
    }
  }

  @Test
  void createsPersistsAndReadsDiagnosisWithPhotoOnDisk() throws Exception {
    UUID ownerId = UUID.randomUUID();
    Parcel parcel = savedParcel(ownerId);
    when(aiServiceClient.predict(any(), eq("potato"))).thenReturn(prediction());

    var creation =
        mockMvc
            .perform(
                multipart("/diagnoses")
                    .file(jpeg())
                    .part(parcelPart(parcel.getId()))
                    .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.parcelId").value(parcel.getId().toString()))
            .andExpect(jsonPath("$.status").value("confident"))
            .andExpect(jsonPath("$.topPredictions[0].disease").value("early_blight"))
            .andExpect(jsonPath("$.modelVersion").value("plant-v1"))
            .andReturn();

    UUID diagnosisId =
        UUID.fromString(
            com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(creation.getResponse().getContentAsString())
                .get("id")
                .asText());
    Diagnosis savedDiagnosis = diagnosisRepository.findById(diagnosisId).orElseThrow();
    uploadedPhoto = Path.of(savedDiagnosis.getPhotoPath());
    assertThat(Files.readAllBytes(uploadedPhoto)).startsWith(jpeg().getBytes());
    assertThat(savedDiagnosis.getPhotoPath()).contains(uploadedPhoto.getFileName().toString());
    verify(aiServiceClient).predict(any(), eq("potato"));

    mockMvc
        .perform(
            get("/diagnoses")
                .param("parcelId", parcel.getId().toString())
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(diagnosisId.toString()));

    mockMvc
        .perform(
            get("/diagnoses/{id}", diagnosisId)
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(diagnosisId.toString()));
  }

  @Test
  void rejectsBadImageAndUnknownParcelAsProblemDetails() throws Exception {
    UUID ownerId = UUID.randomUUID();
    Parcel parcel = savedParcel(ownerId);

    mockMvc
        .perform(
            multipart("/diagnoses")
                .file(
                    new MockMultipartFile(
                        "image", "leaf.txt", MediaType.TEXT_PLAIN_VALUE, "not an image".getBytes()))
                .part(parcelPart(parcel.getId()))
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400));

    mockMvc
        .perform(
            multipart("/diagnoses")
                .file(jpeg())
                .part(parcelPart(UUID.randomUUID()))
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404));

    verifyNoInteractions(aiServiceClient);
  }

  @Test
  void returns413ForFilesLargerThanTenMegabytes() throws Exception {
    UUID ownerId = UUID.randomUUID();
    Parcel parcel = savedParcel(ownerId);
    byte[] tooLarge = new byte[10 * 1024 * 1024 + 1];
    tooLarge[0] = (byte) 0xff;
    tooLarge[1] = (byte) 0xd8;
    tooLarge[2] = (byte) 0xff;

    mockMvc
        .perform(
            multipart("/diagnoses")
                .file(
                    new MockMultipartFile(
                        "image", "leaf.jpg", MediaType.IMAGE_JPEG_VALUE, tooLarge))
                .part(parcelPart(parcel.getId()))
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(413));

    verifyNoInteractions(aiServiceClient);
  }

  @Test
  void doesNotPersistPhotoOrDiagnosisWhenAiServiceFails() throws Exception {
    UUID ownerId = UUID.randomUUID();
    Parcel parcel = savedParcel(ownerId);
    when(aiServiceClient.predict(any(), eq("potato")))
        .thenThrow(new AiServiceException("AI mock failed"));
    long diagnosisCountBefore =
        diagnosisRepository.findByParcelOwnerIdOrderByCreatedAtDesc(ownerId).size();
    long photoCountBefore = photoCount();

    mockMvc
        .perform(
            multipart("/diagnoses")
                .file(jpeg())
                .part(parcelPart(parcel.getId()))
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isBadGateway())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(502));

    assertThat(diagnosisRepository.findByParcelOwnerIdOrderByCreatedAtDesc(ownerId))
        .hasSize((int) diagnosisCountBefore);
    assertThat(photoCount()).isEqualTo(photoCountBefore);
  }

  @Test
  void savesCropNotSupportedResultAsAValidDiagnosis() throws Exception {
    UUID ownerId = UUID.randomUUID();
    Parcel parcel = savedParcel(ownerId);
    when(aiServiceClient.predict(any(), eq("potato")))
        .thenReturn(
            new PredictionResponse(
                "crop_not_supported", "potato", null, List.of(), "not_supported", "plant-v1"));

    var creation =
        mockMvc
            .perform(
                multipart("/diagnoses")
                    .file(jpeg())
                    .part(parcelPart(parcel.getId()))
                    .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("crop_not_supported"))
            .andExpect(jsonPath("$.cropCheck").doesNotExist())
            .andExpect(jsonPath("$.supportLevel").value("not_supported"))
            .andExpect(jsonPath("$.topPredictions").isEmpty())
            .andReturn();

    uploadedPhoto = savedPhotoPath(creation.getResponse().getContentAsString());
  }

  @Test
  void savesResultWithNullCropAsAValidDiagnosis() throws Exception {
    UUID ownerId = UUID.randomUUID();
    Parcel parcel = savedParcel(ownerId);
    when(aiServiceClient.predict(any(), eq("potato")))
        .thenReturn(
            new PredictionResponse(
                "invalid_image", null, null, List.of(), "not_supported", "plant-v1"));

    var creation =
        mockMvc
            .perform(
                multipart("/diagnoses")
                    .file(jpeg())
                    .part(parcelPart(parcel.getId()))
                    .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("invalid_image"))
            .andExpect(jsonPath("$.crop").doesNotExist())
            .andExpect(jsonPath("$.topPredictions").isEmpty())
            .andReturn();

    uploadedPhoto = savedPhotoPath(creation.getResponse().getContentAsString());
  }

  @Test
  void returns502WhenAiResponseIsIncomplete() throws Exception {
    UUID ownerId = UUID.randomUUID();
    Parcel parcel = savedParcel(ownerId);
    when(aiServiceClient.predict(any(), eq("potato")))
        .thenReturn(new PredictionResponse(null, "potato", "ok", List.of(), "lab_only", null));
    mockMvc
        .perform(
            multipart("/diagnoses")
                .file(jpeg())
                .part(parcelPart(parcel.getId()))
                .with(jwt().jwt(token -> token.subject(ownerId.toString()))))
        .andExpect(status().isBadGateway())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(502))
        .andExpect(jsonPath("$.title").value("Bad Gateway"));
  }

  @Test
  void rejectsInvalidBearerTokenWithUnauthorizedNotServerError() throws Exception {
    mockMvc
        .perform(
            multipart("/diagnoses")
                .file(jpeg())
                .part(parcelPart(UUID.randomUUID()))
                .header("Authorization", "Bearer not.a.valid.token"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(aiServiceClient);
  }

  private Parcel savedParcel(UUID ownerId) {
    UserAccount owner =
        userRepository.save(
            new UserAccount(
                ownerId,
                "owner-" + ownerId + "@example.com",
                "hashed-password",
                Instant.now().truncatedTo(ChronoUnit.MILLIS)));
    return parcelRepository.save(
        new Parcel(
            UUID.randomUUID(),
            owner,
            "North field",
            "potato",
            squarePolygon(),
            Instant.now().truncatedTo(ChronoUnit.MILLIS)));
  }

  private static MockMultipartFile jpeg() {
    return new MockMultipartFile(
        "image",
        "leaf.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01, 0x02});
  }

  private static MockPart parcelPart(UUID parcelId) {
    MockPart part = new MockPart("parcelId", parcelId.toString().getBytes(StandardCharsets.UTF_8));
    part.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    return part;
  }

  private static PredictionResponse prediction() {
    return new PredictionResponse(
        "confident",
        "potato",
        "ok",
        List.of(new Prediction("potato_early_blight", "early_blight", 0.92)),
        "lab_only",
        "plant-v1");
  }

  private Path savedPhotoPath(String creationResponseBody) throws Exception {
    UUID diagnosisId =
        UUID.fromString(
            com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(creationResponseBody)
                .get("id")
                .asText());
    return Path.of(diagnosisRepository.findById(diagnosisId).orElseThrow().getPhotoPath());
  }

  private long photoCount() throws Exception {
    if (!Files.exists(UPLOAD_DIRECTORY)) {
      return 0;
    }
    try (var files = Files.list(UPLOAD_DIRECTORY)) {
      return files.count();
    }
  }

  private static Polygon squarePolygon() {
    Coordinate[] ring = {
      new Coordinate(23.59, 46.77),
      new Coordinate(23.60, 46.77),
      new Coordinate(23.60, 46.78),
      new Coordinate(23.59, 46.78),
      new Coordinate(23.59, 46.77)
    };
    Polygon polygon = GEOMETRY_FACTORY.createPolygon(ring);
    polygon.setSRID(SpatialReference.WGS84_SRID);
    return polygon;
  }
}
