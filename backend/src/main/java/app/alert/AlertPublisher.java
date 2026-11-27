package app.alert;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Publishes high-risk alerts for asynchronous delivery. */
@Component
@ConditionalOnProperty(name = "app.alert.enabled", havingValue = "true", matchIfMissing = true)
public class AlertPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final String exchange;
  private final String routingKey;

  public AlertPublisher(
      RabbitTemplate rabbitTemplate,
      @Value("${app.alert.exchange}") String exchange,
      @Value("${app.alert.routing-key}") String routingKey) {
    this.rabbitTemplate = rabbitTemplate;
    this.exchange = exchange;
    this.routingKey = routingKey;
  }

  public void publish(AlertMessage message) {
    rabbitTemplate.convertAndSend(exchange, routingKey, message);
  }
}
