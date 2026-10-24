package com.devikapps.vaikaparts.event.consumer;

import com.devikapps.vaikaparts.InfraGenerated;
import com.devikapps.vaikaparts.event.model.InfraEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ message consumer for processing infrastructure events.
 *
 * <p>This component listens to the configured RabbitMQ queue, deserializes incoming JSON messages
 * into {@link InfraEvent} instances, and delegates them to the {@link EventDispatcher} for
 * processing. The listener waits for event processing to finish so RabbitMQ only acknowledges a
 * message after its handler returns successfully.
 *
 * <p><b>Features:</b>
 *
 * <ul>
 *   <li>Automatic JSON deserialization with polymorphic type handling
 *   <li>Synchronous event processing tied to RabbitMQ acknowledgement
 *   <li>Malformed messages rejected without requeue
 *   <li>Processing failures propagated for broker redelivery
 * </ul>
 *
 * <p><b>Required application property:</b>
 *
 * <ul>
 *   <li>{@code spring.rabbitmq.queue} - Queue name to listen on
 * </ul>
 *
 * <p>The consumer uses Jackson's {@code DefaultTyping.NON_FINAL} to deserialize messages to their
 * concrete event types based on the {@code @class} property included in the JSON payload.
 */
@InfraGenerated
@Component
@Slf4j
public class EventConsumer implements Consumer<String> {

  private final EventDispatcher eventHandler;
  private final ObjectMapper objectMapper;

  /**
   * Constructs an EventConsumer with required dependencies and configuration.
   *
   * @param eventHandler the dispatcher responsible for routing events to appropriate handlers
   */
  public EventConsumer(EventDispatcher eventHandler, ObjectMapper om) {
    this.eventHandler = eventHandler;
    this.objectMapper = om;
  }

  /**
   * RabbitMQ listener method that receives messages from the configured queue.
   *
   * <p>This method is invoked automatically by Spring AMQP when a message arrives on the queue
   * specified by {@code spring.rabbitmq.queue}. It delegates to {@link #accept(String)} for
   * processing.
   *
   * @param rawMessage the raw JSON message string from RabbitMQ
   */
  @RabbitListener(
      queues = "${spring.rabbitmq.queue}",
      containerFactory = "eventListenerContainerFactory")
  public void onMessage(String rawMessage) {
    accept(rawMessage);
  }

  /**
   * Processes a raw message string by deserializing and dispatching it before returning.
   *
   * <p>Handler failures propagate to the Rabbit listener container, which prevents acknowledgement
   * and allows the broker's retry policy to redeliver the message. Malformed JSON is rejected
   * without requeue because retrying the same invalid payload cannot make it valid.
   *
   * @param rawMessage the raw JSON message string to process
   */
  @Override
  public void accept(String rawMessage) {
    InfraEvent event = deserialize(rawMessage);
    eventHandler.accept(event);
    log.info("Event dispatched: {}", event.getClass().getSimpleName());
  }

  /**
   * Deserializes a JSON message string into an {@link InfraEvent} instance.
   *
   * <p>Uses the configured ObjectMapper with polymorphic type handling to reconstruct the concrete
   * event type based on the {@code @class} property in the JSON.
   *
   * @param rawMessage the JSON message string to deserialize
   * @return the deserialized InfraEvent instance
   * @throws AmqpRejectAndDontRequeueException if the message cannot be deserialized
   */
  private InfraEvent deserialize(String rawMessage) {
    try {
      InfraEvent event = objectMapper.readValue(rawMessage, InfraEvent.class);
      if (event == null) {
        throw new AmqpRejectAndDontRequeueException("Malformed infrastructure event");
      }
      return event;
    } catch (JsonProcessingException e) {
      throw new AmqpRejectAndDontRequeueException("Malformed infrastructure event", e);
    }
  }
}
