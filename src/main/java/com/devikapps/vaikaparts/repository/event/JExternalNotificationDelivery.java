package com.devikapps.vaikaparts.repository.event;

import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "external_notification_deliveries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JExternalNotificationDelivery {

  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "notification_id", nullable = false)
  private JDemandPublishedNotification notification;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationChannelType channel;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private ProcessStatus status;

  @Column(name = "attempt_nb", nullable = false)
  private int attemptNb;

  @Column(name = "next_attempt_at", nullable = false)
  private LocalDateTime nextAttemptAt;

  @Column(name = "last_error", columnDefinition = "TEXT")
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "delivered_at")
  private LocalDateTime deliveredAt;

  @Column(name = "locked_at")
  private LocalDateTime lockedAt;

  @Column(name = "lock_token")
  private String lockToken;
}
