package app.alert;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declares the durable risk-alert queue and its dead-letter route. */
@Configuration
@ConditionalOnProperty(name = "app.alert.enabled", havingValue = "true", matchIfMissing = true)
public class AlertRabbitConfiguration {

  @Bean
  MessageConverter alertMessageConverter() {
    return new JacksonJsonMessageConverter("app.alert");
  }

  @Bean
  DirectExchange riskAlertExchange(@Value("${app.alert.exchange}") String exchange) {
    return new DirectExchange(exchange);
  }

  @Bean
  DirectExchange riskAlertDeadLetterExchange(
      @Value("${app.alert.dead-letter-exchange}") String exchange) {
    return new DirectExchange(exchange);
  }

  @Bean
  Queue riskAlertQueue(
      @Value("${app.alert.queue}") String queue,
      @Value("${app.alert.dead-letter-exchange}") String deadLetterExchange,
      @Value("${app.alert.dead-letter-routing-key}") String deadLetterRoutingKey) {
    return QueueBuilder.durable(queue)
        .deadLetterExchange(deadLetterExchange)
        .deadLetterRoutingKey(deadLetterRoutingKey)
        .build();
  }

  @Bean
  Queue riskAlertDeadLetterQueue(@Value("${app.alert.dead-letter-queue}") String queue) {
    return QueueBuilder.durable(queue).build();
  }

  @Bean
  Binding riskAlertBinding(
      @Qualifier("riskAlertQueue") Queue riskAlertQueue,
      @Qualifier("riskAlertExchange") DirectExchange riskAlertExchange,
      @Value("${app.alert.routing-key}") String routingKey) {
    return BindingBuilder.bind(riskAlertQueue).to(riskAlertExchange).with(routingKey);
  }

  @Bean
  Binding riskAlertDeadLetterBinding(
      @Qualifier("riskAlertDeadLetterQueue") Queue riskAlertDeadLetterQueue,
      @Qualifier("riskAlertDeadLetterExchange") DirectExchange riskAlertDeadLetterExchange,
      @Value("${app.alert.dead-letter-routing-key}") String routingKey) {
    return BindingBuilder.bind(riskAlertDeadLetterQueue)
        .to(riskAlertDeadLetterExchange)
        .with(routingKey);
  }
}
