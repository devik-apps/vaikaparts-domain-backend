package com.devikapps.vaikaparts.service.outbox;

import com.devikapps.vaikaparts.event.model.InfraEvent;
import com.devikapps.vaikaparts.repository.OutboxRepository;
import com.devikapps.vaikaparts.repository.event.JOutboxEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

  private final OutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;

  public void enqueue(InfraEvent event) {
    if (outboxRepository.existsById(eventId(event))) {
      return;
    }
    var now = LocalDateTime.now();
    outboxRepository.save(
        JOutboxEvent.builder()
            .id(eventId(event))
            .eventType(event.getEventSource())
            .payload(serialize(event))
            .attemptNb(0)
            .nextAttemptAt(now)
            .createdAt(now)
            .build());
  }

  private String eventId(InfraEvent event) {
    try {
      return (String) event.getClass().getMethod("getId").invoke(event);
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException("Outbox event must expose a String id", e);
    }
  }

  private String serialize(InfraEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Unable to serialize outbox event " + event.getEventSource(), e);
    }
  }
}
