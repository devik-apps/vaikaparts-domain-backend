package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import java.math.BigDecimal;

public record ContactUnlockFeeQuoteResponse(
    String paymentMethod,
    BigDecimal amount,
    BigDecimal paymentFee,
    BigDecimal payerFee,
    BigDecimal totalAmount,
    String currency) {

  public ContactUnlockFeeQuoteResponse(
      String paymentMethod,
      BigDecimal amount,
      BigDecimal payerFee,
      BigDecimal totalAmount,
      String currency) {
    this(paymentMethod, amount, BigDecimal.ZERO, payerFee, totalAmount, currency);
  }
}
