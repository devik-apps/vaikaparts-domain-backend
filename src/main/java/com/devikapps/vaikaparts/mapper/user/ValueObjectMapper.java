package com.devikapps.vaikaparts.mapper.user;

import com.devikapps.vaikaparts.model.LatLon;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.repository.model.JLatLon;
import com.devikapps.vaikaparts.repository.model.JLocation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ValueObjectMapper {

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
    return new JLatLon(latLon.latitude(), latLon.longitude());
  }
}
