package com.devikapps.vaikaparts.repository.model.exchange;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.util.StatusAware;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "offers")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
@SuperBuilder
public class JOffer implements StatusAware {
  @Id private String id;

  private String description;

  @ElementCollection
  @CollectionTable(name = "offer_photos", joinColumns = @JoinColumn(name = "offer_id"))
  @Column(name = "bucket_key")
  private List<String> attachedPhotoBucketKeys;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "canceled_at")
  private LocalDateTime canceledAt;

  @Column(name = "suspended_at")
  private LocalDateTime suspendedAt;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "offer_status")
  private PostStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "seller_id", nullable = false)
  private JSeller seller;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "demand_id", nullable = false)
  private JDemand demand;

  @OneToOne(
      mappedBy = "offer",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private JPartInfo partInfo;

  @Override
  public String toString() {
    return format(
        """
        JOffer={
        \tid=%s,
        \tdescription=%s,
        \tattachedPhotos=%s,
        \tstatus=%s,
        \tcreatedAt=%s,
        \tupdatedAt=%s,
        \tsuspendedAt=%s,
        \tcanceledAt=%s,
        \t%s,
        \t%s,
        \t%s
        }\
        """,
        getId(),
        getDescription(),
        attachedPhotoBucketKeys,
        getStatus(),
        getCreatedAt(),
        getUpdatedAt(),
        getSuspendedAt(),
        getCanceledAt(),
        seller,
        partInfo,
        demand);
  }
}
