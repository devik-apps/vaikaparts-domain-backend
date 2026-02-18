package com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDemandRequest(
    @NotBlank(message = "Description is required") String description,
    @NotNull(message = "Part is required") RestPart part) {}
