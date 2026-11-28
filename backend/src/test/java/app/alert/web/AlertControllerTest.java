package app.alert.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.alert.service.AlertSseService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class AlertControllerTest {

  private static final UUID USER_ID = UUID.randomUUID();

  private final AlertSseService alertSseService = mock(AlertSseService.class);
  private final AlertController controller = new AlertController(alertSseService);

  @Test
  void subscribesTheAuthenticatedUserToTheStream() {
    SseEmitter emitter = new SseEmitter();
    when(alertSseService.subscribe(USER_ID)).thenReturn(emitter);
    Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject(USER_ID.toString()).build();

    SseEmitter result = controller.streamAlerts(jwt);

    assertThat(result).isSameAs(emitter);
    verify(alertSseService).subscribe(USER_ID);
  }
}
