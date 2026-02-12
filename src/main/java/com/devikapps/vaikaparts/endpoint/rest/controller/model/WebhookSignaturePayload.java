package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WebhookSignaturePayload(
    @NotNull @NotBlank String payload, @NotNull @NotBlank String providedSignature) {}
