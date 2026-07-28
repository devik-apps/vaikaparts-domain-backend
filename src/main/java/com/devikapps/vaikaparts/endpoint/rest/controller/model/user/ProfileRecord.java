package com.devikapps.vaikaparts.endpoint.rest.controller.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProfileRecord(
    @JsonProperty("id") String id,
    @JsonProperty("email") String email,
    @JsonProperty("phone_number") String phoneNumber,
    @JsonProperty("name") String name,
    @JsonProperty("profile_img_url") String profileImgUrl,
    @JsonProperty("user_metadata") Map<String, Object> userMetadata,
    @JsonProperty("app_metadata") Map<String, Object> appMetadata,
    @JsonProperty("created_at") OffsetDateTime createdAt,
    @JsonProperty("updated_at") OffsetDateTime updatedAt,
    @JsonProperty("deleted_at") OffsetDateTime deletedAt) {}
