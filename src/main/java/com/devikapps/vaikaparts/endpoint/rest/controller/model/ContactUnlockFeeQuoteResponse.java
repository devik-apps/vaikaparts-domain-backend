package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import java.math.BigDecimal;

public record ContactUnlockFeeQuoteResponse(
    String paymentMethod,
    BigDecimal amount,
    BigDecimal payerFee,
    BigDecimal totalAmount,
    String currency) {}
