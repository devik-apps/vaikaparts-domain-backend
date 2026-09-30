package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus;
import java.math.BigDecimal;

public record ContactUnlockResponse(
    String unlockRequestId,
    ContactUnlockStatus status,
    String paymentUrl,
    String paymentPhoneNumber,
    BigDecimal amount,
    String currency) {

  public ContactUnlockResponse(
      String unlockRequestId, ContactUnlockStatus status, String paymentUrl) {
    this(unlockRequestId, status, paymentUrl, null, null, null);
  }
}
