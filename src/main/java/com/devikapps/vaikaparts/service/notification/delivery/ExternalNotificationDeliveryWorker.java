package com.devikapps.vaikaparts.service.notification.delivery;

import com.devikapps.vaikaparts.mapper.NotificationMapper;
import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.ExternalNotificationDeliveryRepository;
import com.devikapps.vaikaparts.repository.event.JExternalNotificationDelivery;
import com.devikapps.vaikaparts.service.notification.NotificationChannel;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Retries seller demand delivery after restarts. Delivery is at least once: a crash after a
 * provider accepts a message but before SUCCESS is stored can cause that channel to receive it
 * again.
 */
@Component
@Slf4j
public class ExternalNotificationDeliveryWorker {

  private static final int MAX_ERROR_LENGTH = 4000;

  private final ExternalNotificationDeliveryRepository repository;
  private final List<NotificationChannel> channels;
  private final ExternalNotificationDeliveryProperties properties;
  private final TransactionTemplate transactionTemplate;
  private final NotificationMapper notificationMapper;

  public ExternalNotificationDeliveryWorker(
      ExternalNotificationDeliveryRepository repository,
      List<NotificationChannel> channels,
      ExternalNotificationDeliveryProperties properties,
      TransactionTemplate transactionTemplate,
      NotificationMapper notificationMapper) {
    this.repository = repository;
    this.channels = channels;
    this.properties = properties;
    this.transactionTemplate = transactionTemplate;
    this.notificationMapper = notificationMapper;
  }

  @Scheduled(fixedDelayString = "${notification-delivery.poll-interval:1s}")
  public void deliverDue() {
    var now = LocalDateTime.now();
    var token = UUID.randomUUID().toString();
    var ids =
        transactionTemplate.execute(
            ignored ->
                repository.claimDue(
                    now,
                    now.minus(properties.getLeaseDuration()),
                    token,
                    properties.getBatchSize()));
    if (ids == null || ids.isEmpty()) return;

    var claimed =
        transactionTemplate.execute(
            ignored -> repository.findAllByLockTokenOrderByCreatedAt(token));
    if (claimed != null) {
      claimed.forEach(this::deliverClaimed);
    }
  }

  private void deliverClaimed(JExternalNotificationDelivery delivery) {
    deliverClaimed(delivery, () -> loadNotification(delivery.getId()));
  }

  void deliverClaimed(
      JExternalNotificationDelivery delivery, Supplier<Notification> notificationSupplier) {
    var now = LocalDateTime.now();
    var lockToken = delivery.getLockToken();
    try {
      var notification = notificationSupplier.get();
      channel(delivery.getChannel()).send(notification);
      if (repository.markSuccess(delivery.getId(), lockToken, now) == 0) {
        log.warn("Ignoring stale success for external notification delivery {}", delivery.getId());
      }
    } catch (Exception e) {
      var attemptNb = delivery.getAttemptNb() + 1;
      var nextAttemptAt = now.plus(backoff(attemptNb));
      var updated =
          repository.reschedule(
              delivery.getId(), lockToken, attemptNb, nextAttemptAt, truncate(e.getMessage()), now);
      log.warn(
          "External notification delivery {} failed; retry {} {}",
          delivery.getId(),
          attemptNb,
          updated == 0 ? "ignored because the claim is stale" : "scheduled",
          e);
    }
  }

  private Notification loadNotification(String deliveryId) {
    return transactionTemplate.execute(
        ignored -> {
          var delivery =
              repository
                  .findById(deliveryId)
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "External notification delivery not found: " + deliveryId));
          return notificationMapper.toDomain(delivery.getNotification());
        });
  }

  private NotificationChannel channel(NotificationChannelType type) {
    return channels.stream()
        .filter(candidate -> candidate.getChannelType() == type)
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("No notification channel configured for " + type));
  }

  private Duration backoff(int attempt) {
    long multiplier = 1L << Math.min(attempt - 1, 30);
    var calculated = properties.getInitialBackoff().multipliedBy(multiplier);
    return calculated.compareTo(properties.getMaxBackoff()) > 0
        ? properties.getMaxBackoff()
        : calculated;
  }

  private String truncate(String message) {
    if (message == null || message.length() <= MAX_ERROR_LENGTH) return message;
    return message.substring(0, MAX_ERROR_LENGTH);
  }
}
