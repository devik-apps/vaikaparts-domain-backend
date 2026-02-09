package com.devikapps.vaikaparts.model.user;

import com.devikapps.vaikaparts.model.LatLon;
import com.devikapps.vaikaparts.model.Location;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString
public final class Seller extends User {
  private String garageName;
  private Location location;
  private LatLon latLon;
}
