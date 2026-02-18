package com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOfferRequest(
    @NotBlank(message = "Demand ID is required") String demandId,
    @NotBlank(message = "Description is required") String description,
    @Valid @NotNull(message = "Part info is required") RestPartInfo partInfo) {}
