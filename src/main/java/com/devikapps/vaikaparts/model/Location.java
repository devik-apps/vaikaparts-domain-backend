package com.devikapps.vaikaparts.model;

import static com.devikapps.vaikaparts.model.classifier.City.ANTANANARIVO;
import static com.devikapps.vaikaparts.model.classifier.Region.ANALAMANGA;
import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.City;
import com.devikapps.vaikaparts.model.classifier.Region;
import org.jetbrains.annotations.NotNull;

public record Location(@NotNull City city, @NotNull Region region, @NotNull String address) {
  public static Location getDefault() {
    return new Location(ANTANANARIVO, ANALAMANGA, "Anosy");
  }

  @NotNull
  @Override
  public String toString() {
    return format("{ city=%s, region=%s, address=%s }", city, region, address);
  }
}
