package com.devikapps.vaikaparts.endpoint.rest.controller.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseWebhook(
    @JsonProperty("type") String type,
    @JsonProperty("table") String table,
    @JsonProperty("schema") String schema,
    @JsonProperty("record") ProfileRecord record,
    @JsonProperty("old_record") ProfileRecord oldRecord) {}
