package com.devikapps.vaikaparts.repository.event;

import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(of = "id")
public class JDemandPublishedNotification {

  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notification_requested_id")
  private JDemandPublishedNotificationRequested notificationRequested;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_user_id", nullable = false)
  private JUser recipient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "demand_id")
  private JDemand demand;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "offer_id")
  private JOffer offer;

  @Column(name = "message", nullable = false, columnDefinition = "TEXT")
  private String message;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "notification_type", nullable = false)
  private NotificationType notificationType;

  @Column(name = "is_read", nullable = false)
  private boolean read;

  @Column(name = "click_action", columnDefinition = "TEXT")
  private String clickAction;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "read_at")
  private LocalDateTime readAt;
}
