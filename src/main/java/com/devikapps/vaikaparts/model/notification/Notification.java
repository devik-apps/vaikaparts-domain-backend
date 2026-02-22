package com.devikapps.vaikaparts.model.notification;

import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.user.Seller;
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
  private Seller seller;
  private Demand demand;
  private String message;
  private NotificationType notificationType;
  private boolean read;
  private String clickAction;
  private LocalDateTime createdAt;
  private LocalDateTime readAt;
}
