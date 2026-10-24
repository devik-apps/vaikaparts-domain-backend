package com.devikapps.vaikaparts.service.notification.delivery;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.EMAIL;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.mapper.NotificationMapper;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.ExternalNotificationDeliveryRepository;
import com.devikapps.vaikaparts.repository.event.JExternalNotificationDelivery;
import com.devikapps.vaikaparts.service.notification.NotificationChannel;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

class ExternalNotificationDeliveryWorkerTest {

  @Test
  void marks_delivery_success_only_after_channel_returns() {
    var email = mock(NotificationChannel.class);
    var repository = mock(ExternalNotificationDeliveryRepository.class);
    when(email.getChannelType()).thenReturn(EMAIL);
    when(repository.markSuccess(anyString(), anyString(), any())).thenReturn(1);
    var worker =
        new ExternalNotificationDeliveryWorker(
            repository,
            List.of(email),
            properties(),
            mock(TransactionTemplate.class),
            mock(NotificationMapper.class));
    var delivery = pendingDelivery();
    var notification = Notification.builder().id("notification-1").build();

    worker.deliverClaimed(delivery, () -> notification);

    verify(email).send(notification);
    verify(repository).markSuccess(eq(delivery.getId()), eq("claim"), any());
  }

  @Test
  void reschedules_failed_delivery_with_backoff() {
    var email = mock(NotificationChannel.class);
    var repository = mock(ExternalNotificationDeliveryRepository.class);
    when(email.getChannelType()).thenReturn(EMAIL);
    var notification = Notification.builder().id("notification-1").build();
    doThrow(new IllegalStateException("smtp unavailable")).when(email).send(notification);
    when(repository.reschedule(anyString(), anyString(), anyInt(), any(), any(), any()))
        .thenReturn(1);
    var worker =
        new ExternalNotificationDeliveryWorker(
            repository,
            List.of(email),
            properties(),
            mock(TransactionTemplate.class),
            mock(NotificationMapper.class));
    var delivery = pendingDelivery();
    var dueAt = delivery.getNextAttemptAt();

    worker.deliverClaimed(delivery, () -> notification);

    var nextAttempt = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
    verify(repository)
        .reschedule(
            eq(delivery.getId()),
            eq("claim"),
            eq(1),
            nextAttempt.capture(),
            eq("smtp unavailable"),
            any());
    assertTrue(nextAttempt.getValue().isAfter(dueAt));
  }

  @Test
  void reschedules_delivery_when_notification_mapping_fails() {
    var email = mock(NotificationChannel.class);
    var repository = mock(ExternalNotificationDeliveryRepository.class);
    when(email.getChannelType()).thenReturn(EMAIL);
    when(repository.reschedule(anyString(), anyString(), anyInt(), any(), any(), any()))
        .thenReturn(1);
    var worker =
        new ExternalNotificationDeliveryWorker(
            repository,
            List.of(email),
            properties(),
            mock(TransactionTemplate.class),
            mock(NotificationMapper.class));
    var delivery = pendingDelivery();

    worker.deliverClaimed(
        delivery,
        () -> {
          throw new IllegalStateException("mapping failed");
        });

    verify(repository)
        .reschedule(eq(delivery.getId()), eq("claim"), eq(1), any(), eq("mapping failed"), any());
    verify(email, org.mockito.Mockito.never()).send(any());
  }

  private ExternalNotificationDeliveryProperties properties() {
    return new ExternalNotificationDeliveryProperties(
        100, Duration.ofSeconds(2), Duration.ofMinutes(5), Duration.ofSeconds(30));
  }

  private JExternalNotificationDelivery pendingDelivery() {
    var now = LocalDateTime.of(2026, 1, 1, 0, 0);
    return JExternalNotificationDelivery.builder()
        .id("notification-1:EMAIL")
        .channel(EMAIL)
        .status(com.devikapps.vaikaparts.model.classifier.ProcessStatus.PROCESSING)
        .attemptNb(0)
        .nextAttemptAt(now)
        .createdAt(now)
        .lockedAt(now)
        .lockToken("claim")
        .build();
  }
}
