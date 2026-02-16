package com.devikapps.vaikaparts.model;

import java.net.URL;
import java.time.Year;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Part(
    @NotNull String id,
    @NotNull String name,
    String carBrand,
    String carModel,
    Year carYear,
    @Nullable URL imageUrl,
    @NotNull PartCategory partCategory) {}
