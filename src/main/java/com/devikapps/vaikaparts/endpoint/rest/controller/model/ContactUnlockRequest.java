package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import jakarta.validation.constraints.NotBlank;

public record ContactUnlockRequest(
    @NotBlank String provider, String paymentMethod, String temporaryCode) {

  public ContactUnlockRequest(String provider) {
    this(provider, null, null);
  }
}
