package com.devikapps.vaikaparts.service;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.config.SupabaseConf;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.SupabaseEventType;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.SupabaseWebhook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseAuthWebhookService {

  private static final String WEBHOOK_PROCESSED_MESSAGE = "Webhook processed successfully";

  private final UserSyncService userSyncService;
  private final SupabaseConf conf;
  private final ObjectMapper om;

  public Map<String, String> handleAuthWebhook(String rawPayload, String signature) {
    log.info("Processing Supabase Database webhook");

    validateWebhookSignature(signature);
    SupabaseWebhook webhook = parseWebhookPayload(rawPayload);
    String userId = processWebhookEvent(webhook);

    log.info(
        "Successfully processed {} event for user {}", forJava(webhook.type()), forJava(userId));
    return buildSuccessResponse(userId, webhook.type());
  }

  private void validateWebhookSignature(String signature) {
    if (!conf.getWebhookSecret().equals(signature))
      throw new SecurityException("Webhook Signature is invalid");
  }

  private SupabaseWebhook parseWebhookPayload(String rawPayload) {
    try {
      SupabaseWebhook webhook = om.readValue(rawPayload, SupabaseWebhook.class);
      log.debug("Parsed webhook event type: {}", webhook.type());
      return webhook;
    } catch (JsonProcessingException e) {
      log.error("Failed to parse webhook payload: {}", e.getMessage());
      throw new IllegalArgumentException("Invalid webhook payload format", e);
    }
  }

  private String processWebhookEvent(SupabaseWebhook webhook) {
    var eventType = SupabaseEventType.fromValue(webhook.type());

    var profileId =
        eventType == SupabaseEventType.USER_DELETED
            ? webhook.oldRecord().id()
            : webhook.record().id();

    switch (eventType) {
      case USER_CREATED -> {
        log.info("Processing INSERT event for profile: {}", forJava(profileId));
        userSyncService.handleUserCreated(webhook);
      }
      case USER_UPDATED -> {
        log.info("Processing UPDATE event for profile: {}", forJava(profileId));
        userSyncService.handleUserUpdated(webhook);
      }
      case USER_DELETED -> {
        log.info("Processing DELETE event for profile: {}", forJava(profileId));
        userSyncService.handleUserDeleted(webhook);
      }
    }

    return profileId;
  }

  private Map<String, String> buildSuccessResponse(String userId, String eventType) {
    return Map.of(
        "message", WEBHOOK_PROCESSED_MESSAGE,
        "userId", userId,
        "eventType", eventType);
  }
}
