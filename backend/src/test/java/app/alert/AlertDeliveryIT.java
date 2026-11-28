package app.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import app.AbstractIntegrationTest;
import app.risk.dto.RiskLevel;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AlertDeliveryIT extends AbstractIntegrationTest {

  private static final Duration DEAD_LETTER_TIMEOUT = Duration.ofSeconds(10);
  private static final String ALERT_EXCHANGE = "rootine.alerts";
  private static final String ALERT_ROUTING_KEY = "rootine.risk-alert";
  private static final String DEAD_LETTER_QUEUE = "rootine.risk-alerts.dead";

  @Autowired private RabbitTemplate rabbitTemplate;
  @MockitoBean private JavaMailSender mailSender;

  @Test
  void retriesMailFailureThenDeadLettersTheAlert() {
    doThrow(new MailSendException("SMTP unavailable"))
        .when(mailSender)
        .send(any(SimpleMailMessage.class));
    AlertMessage message =
        new AlertMessage(
            UUID.randomUUID(),
            "farmer@example.com",
            UUID.randomUUID(),
            "North field",
            "potato",
            RiskLevel.HIGH,
            78,
            "High risk conditions were detected.",
            Instant.parse("2026-11-20T06:00:00Z"));

    rabbitTemplate.convertAndSend(ALERT_EXCHANGE, ALERT_ROUTING_KEY, message);

    verify(mailSender, timeout(15_000).times(3)).send(any(SimpleMailMessage.class));
    AlertMessage rejected = receiveDeadLetter(message);
    assertThat(rejected)
        .as("alert is dead-lettered after all email delivery attempts fail")
        .isNotNull()
        .isEqualTo(message);
  }

  private AlertMessage receiveDeadLetter(AlertMessage expected) {
    List<AlertMessage> unrelatedMessages = new ArrayList<>();
    long deadline = System.nanoTime() + DEAD_LETTER_TIMEOUT.toNanos();
    try {
      while (System.nanoTime() < deadline) {
        Object received = rabbitTemplate.receiveAndConvert(DEAD_LETTER_QUEUE, 500);
        if (received instanceof AlertMessage alertMessage) {
          if (alertMessage.equals(expected)) {
            return alertMessage;
          }
          unrelatedMessages.add(alertMessage);
        } else if (received != null) {
          throw new AssertionError("Unexpected message in the risk-alert dead-letter queue.");
        }
      }
      return null;
    } finally {
      for (AlertMessage unrelatedMessage : unrelatedMessages) {
        rabbitTemplate.convertAndSend(
            "rootine.alerts.dead", "rootine.risk-alert.dead", unrelatedMessage);
      }
    }
  }
}
