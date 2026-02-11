package com.devikapps.vaikaparts.model;

public record LatLon(double latitude, double longitude) {
  private static final double TANA_LAT = -18.9137;
  private static final double TANA_LON = 47.5361;

  public static LatLon getDefault() {
    return new LatLon(TANA_LAT, TANA_LON);
  }
}
