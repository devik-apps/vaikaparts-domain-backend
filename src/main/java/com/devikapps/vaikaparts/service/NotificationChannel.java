package com.devikapps.vaikaparts.service;

import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.notification.DemandPublishedNotification;

public interface NotificationChannel {

  void send(DemandPublishedNotification demandPublishedNotification);

  NotificationChannelType getChannelType();

  boolean isEnabled();
}
