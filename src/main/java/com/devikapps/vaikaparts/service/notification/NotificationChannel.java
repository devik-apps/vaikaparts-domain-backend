package com.devikapps.vaikaparts.service.notification;

import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.notification.Notification;

public interface NotificationChannel {

  void send(Notification notification);

  NotificationChannelType getChannelType();

  boolean isEnabled();
}
