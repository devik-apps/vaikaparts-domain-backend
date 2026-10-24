package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import jakarta.validation.constraints.Size;

public record ManualContactUnlockReviewRequest(
    @Size(max = 255) String paymentReference, @Size(max = 1000) String reviewNote) {}
