package com.devikapps.vaikaparts.model.user;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.LatLon;
import com.devikapps.vaikaparts.model.Location;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public final class Seller extends User {
  private String garageName;
  private Location location;
  private LatLon latLon;

  @Override
  public String toString() {
    return format(
        """
        Seller={
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
        getProfileImgUrl(),
        getUserType(),
        getStatus(),
        getCreatedAt(),
        getUpdatedAt(),
        garageName,
        location,
        latLon);
  }
}
