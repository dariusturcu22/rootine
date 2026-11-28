package app.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import app.alert.dto.AlertEvent;
import app.alert.service.AlertSseService;
import app.risk.dto.RiskLevel;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class AlertConsumerTest {

  private static final Instant GENERATED_AT = Instant.parse("2026-11-20T06:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PARCEL_ID = UUID.randomUUID();

  private final JavaMailSender mailSender = mock(JavaMailSender.class);
  private final AlertSseService alertSseService = mock(AlertSseService.class);
  private final AlertConsumer consumer =
      new AlertConsumer(mailSender, alertSseService, "no-reply@rootine.local");

  @Test
  void sendsTheFullRiskAlertAndPublishesItsSseEvent() {
    consumer.deliver(message());

    ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(mail.capture());
    assertThat(mail.getValue().getFrom()).isEqualTo("no-reply@rootine.local");
    assertThat(mail.getValue().getTo()).containsExactly("farmer@example.com");
    assertThat(mail.getValue().getSubject()).contains("North field");
    assertThat(mail.getValue().getText())
        .contains(
            "North field", "potato", "high", "78", "High risk conditions", GENERATED_AT.toString());

    verify(alertSseService)
        .publish(
            USER_ID,
            new AlertEvent(
                PARCEL_ID,
                "North field",
                "potato",
                RiskLevel.HIGH,
                78,
                "High risk conditions were detected.",
                GENERATED_AT));
  }

  @Test
  void propagatesMailFailureSoRabbitMqRetriesAndDoesNotPublishSse() {
    org.mockito.Mockito.doThrow(new MailSendException("SMTP unavailable"))
        .when(mailSender)
        .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

    assertThatThrownBy(() -> consumer.deliver(message())).isInstanceOf(MailSendException.class);

    verify(alertSseService, never())
        .publish(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  private static AlertMessage message() {
    return new AlertMessage(
        USER_ID,
        "farmer@example.com",
        PARCEL_ID,
        "North field",
        "potato",
        RiskLevel.HIGH,
        78,
        "High risk conditions were detected.",
        GENERATED_AT);
  }
}
