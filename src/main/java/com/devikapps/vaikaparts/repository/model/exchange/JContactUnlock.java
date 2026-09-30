package com.devikapps.vaikaparts.repository.model.exchange;

import com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contact_unlocks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JContactUnlock {

  @Id
  @Column(name = "unlock_request_id")
  private String unlockRequestId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "offer_id", nullable = false)
  private JOffer offer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "buyer_id", nullable = false)
  private JResearcher buyer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seller_id", nullable = false)
  private JSeller seller;

  @Column(name = "payment_id", unique = true)
  private String paymentId;

  @Column(name = "release_event_id", unique = true)
  private String releaseEventId;

  @Column(nullable = false, length = 50)
  private String provider;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private ContactUnlockStatus status;

  @Column(name = "payment_url")
  private String paymentUrl;

  @Column(name = "paid_at")
  private OffsetDateTime paidAt;

  @Column(name = "released_at")
  private OffsetDateTime releasedAt;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
