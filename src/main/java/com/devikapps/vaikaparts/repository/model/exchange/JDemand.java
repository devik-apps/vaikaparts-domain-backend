package com.devikapps.vaikaparts.repository.model.exchange;

import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
@Table(name = "demands")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
@SuperBuilder
public class JDemand {
  @Id private String id;

  private String description;

  @ElementCollection
  @CollectionTable(name = "demand_photos", joinColumns = @JoinColumn(name = "demand_id"))
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
  @Column(name = "demand_status")
  private PostStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "researcher_id", nullable = false)
  private JResearcher researcher;

  @OneToOne(
      mappedBy = "demand",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private JPart part;

  @OneToMany(mappedBy = "demand", fetch = FetchType.LAZY)
  private List<JOffer> offers = new ArrayList<>();
}
