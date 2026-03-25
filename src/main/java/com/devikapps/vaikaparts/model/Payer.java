package com.devikapps.vaikaparts.model;

import jakarta.validation.constraints.NotNull;

public record Payer(@NotNull String phoneNumber, @NotNull String name) {}
