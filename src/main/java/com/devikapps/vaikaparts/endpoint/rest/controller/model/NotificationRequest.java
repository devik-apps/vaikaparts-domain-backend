package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.devikapps.vaikaparts.model.classifier.NotificationType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NotificationRequest {
  private final String recipientUserId;
  private final String resourceId;
  private final String message;
  private final NotificationType notificationType;
  private final String clickAction;
  private String notificationRequestedId;
}
