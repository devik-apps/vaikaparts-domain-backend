package com.devikapps.vaikaparts.model.notification;

import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.exchange.Exchange;
import com.devikapps.vaikaparts.model.user.User;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
public class Notification {
  private String id;
  private String notificationRequestedId;
  private User recipient;
  private Exchange resource;
  private String message;
  private NotificationType notificationType;
  private boolean
      read; // This is named as "read" because Mapstruct struggles to handle the "isRead" name
  private String clickAction;
  private LocalDateTime createdAt;
  private LocalDateTime readAt;
}
