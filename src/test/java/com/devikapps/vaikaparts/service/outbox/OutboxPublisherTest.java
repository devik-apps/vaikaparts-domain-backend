package com.devikapps.vaikaparts.service.outbox;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.devikapps.vaikaparts.repository.event.JOutboxEvent;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class OutboxPublisherTest {

  @Test
  void rejects_negative_broker_confirm() {
    var rabbitTemplate = mock(RabbitTemplate.class);
    var publisher = new OutboxPublisher(rabbitTemplate, properties(), "events", "event");
    doAnswer(
            invocation -> {
              CorrelationData correlation = invocation.getArgument(3);
              correlation.getFuture().complete(new CorrelationData.Confirm(false, "broker nack"));
              return null;
            })
        .when(rabbitTemplate)
        .convertAndSend(eq("events"), eq("event"), eq("{}"), any(CorrelationData.class));

    var exception =
        assertThrows(
            IllegalStateException.class,
            () -> publisher.publish(JOutboxEvent.builder().id("event-1").payload("{}").build()));

    assertTrue(exception.getMessage().contains("broker nack"));
  }

  private OutboxProperties properties() {
    return new OutboxProperties(
        100,
        Duration.ofMillis(100),
        Duration.ofSeconds(2),
        Duration.ofMinutes(5),
        Duration.ofSeconds(30));
  }
}
