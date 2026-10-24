package com.devikapps.vaikaparts.service.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.devikapps.vaikaparts.repository.OutboxRepository;
import com.devikapps.vaikaparts.repository.event.JOutboxEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class OutboxWorkerTest {

  @Test
  void marks_event_published_only_after_broker_confirm() {
    var publisher = mock(OutboxPublisher.class);
    var repository = mock(OutboxRepository.class);
    var transactionTemplate = executingTransactionTemplate();
    var worker = new OutboxWorker(repository, publisher, properties(), transactionTemplate);
    var event = pendingEvent();

    worker.publishClaimed(event);

    assertNotNull(event.getPublishedAt());
    assertNull(event.getLockToken());
    assertEquals(0, event.getAttemptNb());
    verify(repository).markPublished(eq("event-1"), eq("claim"), eq(event.getPublishedAt()));
  }

  @Test
  void reschedules_event_with_backoff_when_publish_fails() {
    var publisher = mock(OutboxPublisher.class);
    var repository = mock(OutboxRepository.class);
    var transactionTemplate = executingTransactionTemplate();
    var worker = new OutboxWorker(repository, publisher, properties(), transactionTemplate);
    var event = pendingEvent();
    var dueAt = event.getNextAttemptAt();
    doThrow(new IllegalStateException("rabbit unavailable")).when(publisher).publish(event);

    worker.publishClaimed(event);

    assertNull(event.getPublishedAt());
    assertNull(event.getLockToken());
    assertEquals(1, event.getAttemptNb());
    assertTrue(event.getNextAttemptAt().isAfter(dueAt));
    assertEquals("rabbit unavailable", event.getLastError());
    verify(repository)
        .reschedule("event-1", "claim", 1, event.getNextAttemptAt(), "rabbit unavailable");
  }

  private TransactionTemplate executingTransactionTemplate() {
    var transactionTemplate = mock(TransactionTemplate.class);
    org.mockito.Mockito.doAnswer(
            invocation -> {
              Consumer<TransactionStatus> callback = invocation.getArgument(0);
              callback.accept(null);
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(org.mockito.ArgumentMatchers.any());
    return transactionTemplate;
  }

  private OutboxProperties properties() {
    return new OutboxProperties(
        100,
        Duration.ofSeconds(5),
        Duration.ofSeconds(2),
        Duration.ofMinutes(5),
        Duration.ofSeconds(30));
  }

  private JOutboxEvent pendingEvent() {
    var now = LocalDateTime.of(2026, 1, 1, 0, 0);
    return JOutboxEvent.builder()
        .id("event-1")
        .eventType("DemandPublishedRequested")
        .payload("{}")
        .attemptNb(0)
        .nextAttemptAt(now)
        .createdAt(now)
        .lockToken("claim")
        .lockedAt(now)
        .build();
  }
}
