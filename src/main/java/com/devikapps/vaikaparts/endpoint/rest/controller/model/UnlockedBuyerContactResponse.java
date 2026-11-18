package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UnlockedBuyerContactResponse(
    String name,
    @JsonProperty("phone_number") String phoneNumber,
    String email) {}
