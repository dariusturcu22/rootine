package app.diagnosis.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.Application;
import app.common.SupportLevel;
import app.diagnosis.dto.CropCheck;
import app.diagnosis.dto.DiagnosisResponse;
import app.diagnosis.dto.DiagnosisStatus;
import app.diagnosis.dto.Prediction;
import app.diagnosis.service.DiagnosisService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest(
    classes = Application.class,
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
          + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
          + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
          + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
          + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration,"
          + "org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration"
    })
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DiagnosisControllerTest {

  private static final UUID OWNER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final UUID PARCEL_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
  private static final UUID DIAGNOSIS_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174002");

  @Autowired private MockMvc mockMvc;
  @MockitoBean private DiagnosisService diagnosisService;

  @Test
  void postsMultipartImageAndParcelId() throws Exception {
    given(diagnosisService.createDiagnosis(eq(OWNER_ID), any(MultipartFile.class), eq(PARCEL_ID)))
        .willReturn(response());

    mockMvc
        .perform(
            multipart("/diagnoses")
                .file(
                    new MockMultipartFile(
                        "image", "leaf.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {(byte) 0xff}))
                .part(parcelPart(PARCEL_ID))
                .with(jwt().jwt(token -> token.subject(OWNER_ID.toString()))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(DIAGNOSIS_ID.toString()))
        .andExpect(jsonPath("$.parcelId").value(PARCEL_ID.toString()))
        .andExpect(jsonPath("$.status").value("confident"));
  }

  @Test
  void listsAndGetsDiagnosesForTheCurrentUser() throws Exception {
    given(diagnosisService.listDiagnoses(OWNER_ID, PARCEL_ID)).willReturn(List.of(response()));
    given(diagnosisService.getDiagnosis(OWNER_ID, DIAGNOSIS_ID)).willReturn(response());

    mockMvc
        .perform(
            get("/diagnoses")
                .param("parcelId", PARCEL_ID.toString())
                .with(jwt().jwt(token -> token.subject(OWNER_ID.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(DIAGNOSIS_ID.toString()));

    mockMvc
        .perform(
            get("/diagnoses/{id}", DIAGNOSIS_ID)
                .with(jwt().jwt(token -> token.subject(OWNER_ID.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(DIAGNOSIS_ID.toString()));
  }

  private static DiagnosisResponse response() {
    return new DiagnosisResponse(
        DIAGNOSIS_ID,
        PARCEL_ID,
        DiagnosisStatus.CONFIDENT,
        "potato",
        CropCheck.OK,
        List.of(new Prediction("potato_early_blight", "early_blight", 0.92)),
        SupportLevel.LAB_ONLY,
        "plant-v1",
        Instant.parse("2026-11-01T08:05:00Z"));
  }

  private static MockPart parcelPart(UUID parcelId) {
    MockPart part = new MockPart("parcelId", parcelId.toString().getBytes());
    part.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    return part;
  }
}
