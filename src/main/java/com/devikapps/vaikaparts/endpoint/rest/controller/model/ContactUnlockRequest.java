package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import jakarta.validation.constraints.NotBlank;

public record ContactUnlockRequest(
    @NotBlank String provider, String paymentMethod, String temporaryCode, String paymentPhoneNumber) {

  public ContactUnlockRequest(String provider) {
    this(provider, null, null, null);
  }

  public ContactUnlockRequest(String provider, String paymentMethod, String temporaryCode) {
    this(provider, paymentMethod, temporaryCode, null);
  }
}
