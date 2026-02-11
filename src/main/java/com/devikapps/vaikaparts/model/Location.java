package com.devikapps.vaikaparts.model;

import static com.devikapps.vaikaparts.model.classifier.City.ANTANANARIVO;
import static com.devikapps.vaikaparts.model.classifier.Region.ANALAMANGA;

import com.devikapps.vaikaparts.model.classifier.City;
import com.devikapps.vaikaparts.model.classifier.Region;
import jakarta.validation.constraints.NotNull;

public record Location(@NotNull City city, @NotNull Region region, @NotNull String address) {
  public static Location getDefault() {
    return new Location(ANTANANARIVO, ANALAMANGA, "Anosy");
  }
}
