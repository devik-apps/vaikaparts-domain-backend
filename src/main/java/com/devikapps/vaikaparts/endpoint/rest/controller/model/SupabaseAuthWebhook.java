package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseAuthWebhook(
    @JsonProperty("event") String event,
    @JsonProperty("user") UserData user,
    @JsonProperty("created_at") Instant createdAt) {}
