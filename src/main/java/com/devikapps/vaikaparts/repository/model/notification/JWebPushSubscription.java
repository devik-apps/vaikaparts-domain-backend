package com.devikapps.vaikaparts.repository.model.notification;

import com.devikapps.vaikaparts.repository.model.user.JUser;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "web_push_subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JWebPushSubscription {
  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private JUser user;

  @Column(nullable = false, unique = true, length = 2048)
  private String endpoint;

  @Column(nullable = false, length = 512)
  private String p256dh;

  @Column(nullable = false, length = 512)
  private String auth;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
