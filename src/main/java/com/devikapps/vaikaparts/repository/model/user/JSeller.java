package com.devikapps.vaikaparts.repository.model.user;

import static java.lang.String.format;

import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotificationRequested;
import com.devikapps.vaikaparts.repository.model.JLatLon;
import com.devikapps.vaikaparts.repository.model.JLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
         \tlatLon=%s
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
        latLon);
  }
}
