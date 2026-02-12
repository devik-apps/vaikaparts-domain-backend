package com.devikapps.vaikaparts.service;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.SupabaseAuthWebhook;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.SupabaseEventType;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.WebhookSignaturePayload;
import com.devikapps.vaikaparts.validator.WebhookSignatureValidator;
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
  private final WebhookSignatureValidator webhookSignatureValidator;
  private final ObjectMapper om;

  public Map<String, String> handleAuthWebhook(String rawPayload, String signature) {
    log.info("Processing Supabase Auth webhook");

    validateWebhookSignature(rawPayload, signature);
    SupabaseAuthWebhook webhook = parseWebhookPayload(rawPayload);
    String userId = processWebhookEvent(webhook);

    log.info(
        "Successfully processed {} event for user {}", forJava(webhook.event()), forJava(userId));
    return buildSuccessResponse(userId);
  }

  private void validateWebhookSignature(String payload, String signature) {
    try {
      webhookSignatureValidator.validate(new WebhookSignaturePayload(payload, signature));
    } catch (SecurityException e) {
      log.error("Webhook signature validation failed: {}", e.getMessage());
      throw e;
    }
  }

  private SupabaseAuthWebhook parseWebhookPayload(String rawPayload) {
    try {
      SupabaseAuthWebhook webhook = om.readValue(rawPayload, SupabaseAuthWebhook.class);
      log.debug("Parsed webhook event: {}", webhook.event());
      return webhook;
    } catch (JsonProcessingException e) {
      log.error("Failed to parse webhook payload: {}", e.getMessage());
      throw new IllegalArgumentException("Invalid webhook payload format", e);
    }
  }

  private String processWebhookEvent(SupabaseAuthWebhook webhook) {
    var eventType = SupabaseEventType.fromValue(webhook.event());
    var supabaseUserId = webhook.user().id();

    switch (eventType) {
      case USER_CREATED -> {
        log.info("Processing USER_CREATED event for user: {}", forJava(supabaseUserId));
        userSyncService.handleUserCreated(webhook);
      }
      case USER_UPDATED -> {
        log.info("Processing USER_UPDATED event for user: {}", forJava(supabaseUserId));
        userSyncService.handleUserUpdated(webhook);
      }
      case USER_DELETED -> {
        log.info("Processing USER_DELETED event for user: {}", forJava(supabaseUserId));
        userSyncService.handleUserDeleted(webhook);
      }
    }

    return supabaseUserId;
  }

  private Map<String, String> buildSuccessResponse(String userId) {
    return Map.of(
        "message", WEBHOOK_PROCESSED_MESSAGE,
        "userId", userId);
  }
}
