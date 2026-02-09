package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.model.classifier.City;
import com.devikapps.vaikaparts.model.classifier.Region;
import jakarta.validation.constraints.NotNull;

public record Location(@NotNull City city, @NotNull Region region, @NotNull String address) {}
