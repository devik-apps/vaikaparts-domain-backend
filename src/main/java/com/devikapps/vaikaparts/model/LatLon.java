package com.devikapps.vaikaparts.model;

import static java.lang.String.format;

import org.jetbrains.annotations.NotNull;

public record LatLon(double latitude, double longitude) {
  private static final double TANA_LAT = -18.9137;
  private static final double TANA_LON = 47.5361;

  public static LatLon getDefault() {
    return new LatLon(TANA_LAT, TANA_LON);
  }

  @NotNull
  @Override
  public String toString() {
    return format("{ lat=%f, lon=%f }", latitude, longitude);
  }
}
