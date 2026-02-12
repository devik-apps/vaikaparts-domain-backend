package com.devikapps.vaikaparts.endpoint.rest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.config.SupabaseConf;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class SupabaseAuthWebhookControllerIT extends FacadeIT {

  private static final String WEBHOOK_ENDPOINT = "/webhooks/spb/auth";
  private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
  private static final String TEST_SUPABASE_USER_ID = "550e8400-e29b-41d4-a716-446655440000";
  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_PHONE = "+1234567890";
  private static final String TEST_NAME = "John Doe";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SupabaseConf supabaseConf;
  @Autowired private UserRepository userRepository;

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  void should_return_200_with_success_message_when_valid_webhook() throws Exception {
    val payload = buildUserCreatedPayload();
    val signature = computeSignature(payload);

    mockMvc
        .perform(
            post(WEBHOOK_ENDPOINT)
                .header(SIGNATURE_HEADER, signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Webhook processed successfully"))
        .andExpect(jsonPath("$.userId").value(TEST_SUPABASE_USER_ID));
  }

  @Test
  void should_return_401_when_signature_is_missing() throws Exception {
    var payload = buildUserCreatedPayload();

    mockMvc
        .perform(post(WEBHOOK_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void should_return_401_when_signature_is_invalid() throws Exception {
    val payload = buildUserCreatedPayload();
    val invalidSignature = "invalid-signature";

    mockMvc
        .perform(
            post(WEBHOOK_ENDPOINT)
                .header(SIGNATURE_HEADER, invalidSignature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void should_return_400_when_payload_is_malformed() throws Exception {
    val malformedPayload = "{invalid json";
    val signature = computeSignature(malformedPayload);

    mockMvc
        .perform(
            post(WEBHOOK_ENDPOINT)
                .header(SIGNATURE_HEADER, signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedPayload))
        .andExpect(status().isBadRequest());
  }

  @SneakyThrows
  private String buildUserCreatedPayload() {
    val webhook = new HashMap<String, Object>();
    webhook.put("event", "user.created");
    webhook.put("created_at", "2024-01-01T00:00:00Z");

    val user = new HashMap<String, Object>();
    user.put("id", TEST_SUPABASE_USER_ID);
    user.put("email", TEST_EMAIL);
    user.put("phone", TEST_PHONE);

    val metadata = new HashMap<String, Object>();
    metadata.put("user_type", "RESEARCHER");
    metadata.put("full_name", TEST_NAME);
    user.put("user_metadata", metadata);

    user.put("app_metadata", null);
    user.put("created_at", "2024-01-01T00:00:00Z");
    user.put("updated_at", "2024-01-02T00:00:00Z");
    user.put("deleted_at", null);

    webhook.put("user", user);

    return objectMapper.writeValueAsString(webhook);
  }

  @SneakyThrows
  private String computeSignature(String payload) {
    Mac hmac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey =
        new SecretKeySpec(
            supabaseConf.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    hmac.init(secretKey);
    byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(hash);
  }
}
