package app.alert;

import static org.assertj.core.api.Assertions.assertThat;

import app.risk.dto.RiskLevel;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

class AlertMessageConversionTest {

  @Test
  void convertsAlertsToAndFromJsonForRabbitMq() {
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
    JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter("app.alert");

    AlertMessage converted =
        (AlertMessage) converter.fromMessage(converter.toMessage(message, new MessageProperties()));

    assertThat(converted).isEqualTo(message);
  }
}
