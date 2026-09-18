package com.devikapps.vaikaparts.model.user;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.LatLon;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public final class Seller extends User {
  private String garageName;
  private Location location;
  private LatLon latLon;
  @Builder.Default private List<PartCategory> categoryList = new ArrayList<>();
  @Builder.Default private Boolean handleAllCategory = true;
  @Builder.Default private Boolean isDeliverying = false;

  @Override
  public String toString() {
    return format(
        """
        Seller={
         \tid=%s,
         \tsupabaseUserId=%s,
         \tname=%s,
         \temail=%s,
         \tphoneNumber=%s,
         \tprofileImgUrl=%s,
         \tuserType=%s,
         \tstatus=%s,
         \tcreatedAt=%s,
         \tupdatedAt=%s,
         \tgarageName=%s,
         \tlocation=%s,
         \tlatLon=%s,
         \tcategoryList=%s,
         \thandleAllCategory=%s,
         \tisDeliverying=%s
        }\
        """,
        getId(),
        getSupabaseUserId(),
        getName(),
        getEmail(),
        getPhoneNumber(),
        getProfileImgUrl(),
        getUserType(),
        getStatus(),
        getCreatedAt(),
        getUpdatedAt(),
        garageName,
        location,
        latLon,
        categoryList,
        handleAllCategory,
        isDeliverying);
  }
}
