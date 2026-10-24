package com.devikapps.vaikaparts.service.outbox;

import com.devikapps.vaikaparts.repository.event.JOutboxEvent;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final OutboxProperties properties;

  private final String exchangeName;
  private final String routingKey;

  public OutboxPublisher(
      RabbitTemplate rabbitTemplate,
      OutboxProperties properties,
      @Value("${spring.rabbitmq.exchange}") String exchangeName,
      @Value("${spring.rabbitmq.routing-key}") String routingKey) {
    this.rabbitTemplate = rabbitTemplate;
    this.properties = properties;
    this.exchangeName = exchangeName;
    this.routingKey = routingKey;
  }

  public void publish(JOutboxEvent event) {
    var correlation = new CorrelationData(event.getId());
    rabbitTemplate.convertAndSend(exchangeName, routingKey, event.getPayload(), correlation);
    try {
      var confirm =
          correlation
              .getFuture()
              .get(properties.getConfirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
      if (!confirm.isAck()) {
        throw new IllegalStateException("RabbitMQ rejected event: " + confirm.getReason());
      }
      if (correlation.getReturned() != null) {
        throw new IllegalStateException("RabbitMQ returned unroutable event " + event.getId());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while awaiting RabbitMQ confirm", e);
    } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
      throw new IllegalStateException("RabbitMQ publish was not confirmed", e);
    }
  }
}
