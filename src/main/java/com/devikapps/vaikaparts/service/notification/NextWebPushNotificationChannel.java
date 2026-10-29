package com.devikapps.vaikaparts.service.notification;

import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.service.notification.push.PushDispatchClient;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NextWebPushNotificationChannel implements NotificationChannel {
  private final PushDispatchClient dispatchClient;
  private final WebPushProperties properties;

  @Override
  public void send(Notification notification) {
    dispatchClient.dispatch(notification);
  }

  @Override
  public NotificationChannelType getChannelType() {
    return NotificationChannelType.WEB_PUSH;
  }

  @Override
  public boolean isEnabled() {
    return properties.isEnabled();
  }
}
