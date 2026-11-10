package app.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.Application;
import app.aiclient.AiServiceClient;
import app.diagnosis.service.DiagnosisService;
import app.parcel.service.ParcelService;
import app.risk.service.RiskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = Application.class,
    properties = {
      "spring.autoconfigure.exclude=" + ContractEndpointsTest.EXCLUDED_AUTOCONFIG,
      "app.demo-token.enabled=false"
    })
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DemoTokenDisabledTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AiServiceClient aiServiceClient;
  @MockitoBean private DiagnosisService diagnosisService;
  @MockitoBean private ParcelService parcelService;
  @MockitoBean private RiskService riskService;

  @Test
  void demoTokenEndpointIsNotPublicWhenDemoModeIsDisabled() throws Exception {
    mockMvc.perform(post("/auth/demo-token")).andExpect(status().isUnauthorized());
  }
}
