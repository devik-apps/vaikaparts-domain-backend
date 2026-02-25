package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.model.LatLon;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.repository.event.JNotification;
import com.devikapps.vaikaparts.repository.model.JLatLon;
import com.devikapps.vaikaparts.repository.model.JLocation;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.time.Year;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ValueObjectMapper {
  int DEFAULT_YEAR = 1900;

  default String getNotificationRequestedId(JNotification jNotification) {
    if (jNotification == null) return null;
    return jNotification.getNotificationRequested().getId();
  }

  default Location map(JLocation jLocation) {
    if (jLocation == null) return null;
    return new Location(jLocation.getCity(), jLocation.getRegion(), jLocation.getAddress());
  }

  default JLocation map(Location location) {
    if (location == null) return null;
    return new JLocation(location.city(), location.region(), location.address());
  }

  default LatLon map(JLatLon jLatLon) {
    if (jLatLon == null) return null;
    return new LatLon(jLatLon.getLatitude(), jLatLon.getLongitude());
  }

  default JLatLon map(LatLon latLon) {
    if (latLon == null) return null;
    return new JLatLon(latLon.lat(), latLon.lon());
  }

  default Year intToYear(int year) {
    return year == 0 ? null : Year.of(year);
  }

  default int yearToInt(Year year) {
    return year != null ? year.getValue() : DEFAULT_YEAR;
  }

  default JSeller toPersistenceObj(String sellerId) {
    return JSeller.builder().id(sellerId).build();
  }
}
