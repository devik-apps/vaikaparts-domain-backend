package com.devikapps.vaikaparts.service.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.service.notification.push.PushDispatchClient;
import org.junit.jupiter.api.Test;

class NextWebPushNotificationChannelTest {
  private final PushDispatchClient client = mock(PushDispatchClient.class);
  private final WebPushProperties properties = new WebPushProperties();
  private final NextWebPushNotificationChannel channel =
      new NextWebPushNotificationChannel(client, properties);

  @Test
  void delegatesNotificationToNextDispatchAndUsesWebPushDeliveryType() {
    properties.setEnabled(true);
    var notification =
        Notification.builder()
            .id("notification-123")
            .notificationType(NotificationType.SYSTEM_ANNOUNCEMENT)
            .recipient(Researcher.builder().id("user-1").build())
            .build();

    channel.send(notification);

    verify(client).dispatch(notification);
    assertEquals(NotificationChannelType.WEB_PUSH, channel.getChannelType());
    assertTrue(channel.isEnabled());
  }
}
