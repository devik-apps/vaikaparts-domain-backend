package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.devikapps.vaikaparts.model.classifier.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationRequest {
  private final String sellerId;
  private final String demandId;
  private final String message;
  private final NotificationType notificationType;
  private final String clickAction;
}
