package com.devikapps.vaikaparts.model;

import jakarta.validation.constraints.NotNull;

public record Payer(@NotNull String phoneNumber, @NotNull String name, @NotNull String country) {

  public static Payer of(String phoneNumber, String name) {
    return new Payer(phoneNumber, name, "MADAGASCAR");
  }
}
