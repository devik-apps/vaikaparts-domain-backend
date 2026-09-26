package com.devikapps.vaikaparts.repository.model.user;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.Arrondissement;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotificationRequested;
import com.devikapps.vaikaparts.repository.model.JLatLon;
import com.devikapps.vaikaparts.repository.model.JLocation;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "sellers")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class JSeller extends JUser {
  @Column(name = "garage_name")
  private String garageName;

  @Embedded private JLocation location;

  @Embedded private JLatLon latLon;

  @ElementCollection
  @CollectionTable(name = "seller_categories", joinColumns = @JoinColumn(name = "seller_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "part_category", nullable = false)
  @Builder.Default
  private List<PartCategory> categoryList = new ArrayList<>();

  @Column(name = "handle_all_category", nullable = false)
  @Builder.Default
  private Boolean handleAllCategory = true;

  @Column(name = "is_deliverying", nullable = false)
  @Builder.Default
  private Boolean isDeliverying = false;

  @Column(name = "is_verified", nullable = false)
  @Builder.Default
  private Boolean isVerified = false;

  @Column(name = "arrondissement")
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private Arrondissement arrondissement;

  @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
  @Builder.Default
  private List<JDemandPublishedNotificationRequested> notificationRequestedLogs = new ArrayList<>();

  @Override
  public String toString() {
    return format(
        """
        JSeller={
         \tid=%s,
         \tsupabaseUserId=%s,
         \tname=%s,
         \tphoneNumber=%s,
         \tprofileImgUrl=%s,
         \tuserType=%s,
         \tstatus=%s,
         \tcreatedAt=%s,
         \tupdatedAt=%s,
         \tgarageName=%s,
         \tlocation=%s,
         \tlatLon=%s,
         \tarrondissement=%s,
         \thandleAllCategory=%s,
         \tisDeliverying=%s,
         \tisVerified=%s
        }\
        """,
        getId(),
        getSupabaseUserId(),
        getName(),
        getPhoneNumber(),
        getProfileImgKey(),
        getUserType(),
        getStatus(),
        getCreatedAt(),
        getUpdatedAt(),
        garageName,
        location,
        latLon,
        arrondissement,
        handleAllCategory,
        isDeliverying,
        isVerified);
  }
}
