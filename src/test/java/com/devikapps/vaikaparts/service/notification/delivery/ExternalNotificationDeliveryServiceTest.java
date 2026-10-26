package com.devikapps.vaikaparts.service.notification.delivery;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.EMAIL;
import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.SMS;
import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.WEB_PUSH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.ExternalNotificationDeliveryRepository;
import com.devikapps.vaikaparts.repository.event.JExternalNotificationDelivery;
import com.devikapps.vaikaparts.service.notification.NotificationChannel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ExternalNotificationDeliveryServiceTest {

  @Test
  void enqueues_one_persistent_delivery_per_external_channel() {
    var repository = mock(ExternalNotificationDeliveryRepository.class);
    var notifications = mock(DemandPublishedNotificationRepository.class);
    var email = channel(EMAIL);
    var sms = channel(SMS);
    var service = new ExternalNotificationDeliveryService(repository, notifications);
    var notification = Notification.builder().id("notification-1").build();

    service.enqueue(notification, List.of(email, sms));

    var captor = ArgumentCaptor.forClass(JExternalNotificationDelivery.class);
    verify(repository, times(2)).save(captor.capture());
    assertEquals(
        List.of(EMAIL, SMS),
        captor.getAllValues().stream().map(JExternalNotificationDelivery::getChannel).toList());
  }

  @Test
  void enqueueCreatesOneWebPushDeliveryPerNotification() {
    var repository = mock(ExternalNotificationDeliveryRepository.class);
    var notifications = mock(DemandPublishedNotificationRepository.class);
    var persisted =
        com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification.builder()
            .id("notification-1")
            .build();
    when(notifications.getReferenceById("notification-1")).thenReturn(persisted);
    var service = new ExternalNotificationDeliveryService(repository, notifications);
    var push = channel(WEB_PUSH);
    var notification = Notification.builder().id("notification-1").build();

    service.enqueue(notification, List.of(push));
    service.enqueue(notification, List.of(push));

    var captor = ArgumentCaptor.forClass(JExternalNotificationDelivery.class);
    verify(repository, times(2)).save(captor.capture());
    assertEquals(
        List.of("notification-1:WEB_PUSH", "notification-1:WEB_PUSH"),
        captor.getAllValues().stream().map(JExternalNotificationDelivery::getId).toList());
    captor
        .getAllValues()
        .forEach(
            delivery -> {
              assertEquals(persisted, delivery.getNotification());
              assertEquals(WEB_PUSH, delivery.getChannel());
              assertEquals(
                  com.devikapps.vaikaparts.model.classifier.ProcessStatus.PENDING,
                  delivery.getStatus());
              assertEquals(0, delivery.getAttemptNb());
              assertEquals(delivery.getCreatedAt(), delivery.getNextAttemptAt());
            });
  }

  private NotificationChannel channel(
      com.devikapps.vaikaparts.model.classifier.NotificationChannelType type) {
    var channel = mock(NotificationChannel.class);
    when(channel.getChannelType()).thenReturn(type);
    return channel;
  }
}
