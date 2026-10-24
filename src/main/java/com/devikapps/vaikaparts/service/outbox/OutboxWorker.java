package com.devikapps.vaikaparts.service.outbox;

import com.devikapps.vaikaparts.repository.OutboxRepository;
import com.devikapps.vaikaparts.repository.event.JOutboxEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Slf4j
public class OutboxWorker {

  private static final int MAX_ERROR_LENGTH = 4000;
  private final OutboxRepository outboxRepository;
  private final OutboxPublisher outboxPublisher;
  private final OutboxProperties properties;
  private final TransactionTemplate transactionTemplate;

  public OutboxWorker(
      OutboxRepository outboxRepository,
      OutboxPublisher outboxPublisher,
      OutboxProperties properties,
      TransactionTemplate transactionTemplate) {
    this.outboxRepository = outboxRepository;
    this.outboxPublisher = outboxPublisher;
    this.properties = properties;
    this.transactionTemplate = transactionTemplate;
  }

  @Scheduled(fixedDelayString = "${outbox.poll-interval:1s}")
  public void publishDue() {
    var now = LocalDateTime.now();
    var token = UUID.randomUUID().toString();
    var ids =
        transactionTemplate.execute(
            ignored ->
                outboxRepository.claimDue(
                    now,
                    now.minus(properties.getLeaseDuration()),
                    token,
                    properties.getBatchSize()));
    if (ids == null || ids.isEmpty()) {
      return;
    }
    outboxRepository.findAllByLockTokenOrderByCreatedAt(token).forEach(this::publishClaimed);
  }

  void publishClaimed(JOutboxEvent event) {
    var lockToken = event.getLockToken();
    try {
      outboxPublisher.publish(event);
      event.setPublishedAt(LocalDateTime.now());
      event.setLastError(null);
      transactionTemplate.executeWithoutResult(
          ignored ->
              outboxRepository.markPublished(event.getId(), lockToken, event.getPublishedAt()));
    } catch (Exception e) {
      event.setAttemptNb(event.getAttemptNb() + 1);
      event.setNextAttemptAt(LocalDateTime.now().plus(backoff(event.getAttemptNb())));
      event.setLastError(truncate(e.getMessage()));
      transactionTemplate.executeWithoutResult(
          ignored ->
              outboxRepository.reschedule(
                  event.getId(),
                  lockToken,
                  event.getAttemptNb(),
                  event.getNextAttemptAt(),
                  event.getLastError()));
      log.warn(
          "Outbox publish failed for event {}, retry {} scheduled",
          event.getId(),
          event.getAttemptNb(),
          e);
    } finally {
      event.setLockedAt(null);
      event.setLockToken(null);
    }
  }

  private Duration backoff(int attempt) {
    long multiplier = 1L << Math.min(attempt - 1, 30);
    var calculated = properties.getInitialBackoff().multipliedBy(multiplier);
    return calculated.compareTo(properties.getMaxBackoff()) > 0
        ? properties.getMaxBackoff()
        : calculated;
  }

  private String truncate(String message) {
    if (message == null || message.length() <= MAX_ERROR_LENGTH) {
      return message;
    }
    return message.substring(0, MAX_ERROR_LENGTH);
  }
}
