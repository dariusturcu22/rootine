package app.alert;

import app.alert.dto.AlertEvent;
import app.alert.service.AlertSseService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Delivers queued alerts by email, then publishes them to connected SSE clients. */
@Component
@ConditionalOnProperty(name = "app.alert.enabled", havingValue = "true", matchIfMissing = true)
public class AlertConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(AlertConsumer.class);

  private final JavaMailSender mailSender;
  private final AlertSseService alertSseService;
  private final String mailFrom;

  public AlertConsumer(
      JavaMailSender mailSender,
      AlertSseService alertSseService,
      @Value("${app.mail-from}") String mailFrom) {
    this.mailSender = mailSender;
    this.alertSseService = alertSseService;
    this.mailFrom = mailFrom;
  }

  @RabbitListener(queues = "${app.alert.queue}")
  public void deliver(AlertMessage message) {
    try {
      mailSender.send(toMail(message));
    } catch (MailException exception) {
      LOGGER.warn(
          "Email delivery failed for parcel {}; RabbitMQ will retry the alert",
          message.parcelId(),
          exception);
      throw exception;
    }

    UUID userId = message.userId();
    alertSseService.publish(userId, toEvent(message));
  }

  private SimpleMailMessage toMail(AlertMessage message) {
    SimpleMailMessage mail = new SimpleMailMessage();
    mail.setFrom(mailFrom);
    mail.setTo(message.recipientEmail());
    mail.setSubject("High disease risk: " + message.parcelName());
    mail.setText(
        """
        A high disease risk was detected for your parcel.

        Parcel: %s
        Crop: %s
        Risk level: %s
        Risk index: %d
        Explanation: %s
        Generated at: %s
        """
            .formatted(
                message.parcelName(),
                message.crop(),
                message.level().jsonValue(),
                message.riskIndex(),
                message.reason(),
                message.generatedAt()));
    return mail;
  }

  private static AlertEvent toEvent(AlertMessage message) {
    return new AlertEvent(
        message.parcelId(),
        message.parcelName(),
        message.crop(),
        message.level(),
        message.riskIndex(),
        message.reason(),
        message.generatedAt());
  }
}
