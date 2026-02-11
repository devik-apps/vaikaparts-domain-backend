package com.devikapps.vaikaparts.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.config.SupabaseConf;
import com.devikapps.vaikaparts.mapper.user.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SupabaseAuthWebhookServiceIT extends FacadeIT {

  private static final String TEST_SUPABASE_USER_ID = "550e8400-e29b-41d4-a716-446655440000";
  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_PHONE = "+1234567890";
  private static final String TEST_NAME = "John Doe";
  @Autowired ValueObjectMapper vom;
  @Autowired private SupabaseAuthWebhookService supabaseAuthWebhookService;
  @Autowired private ObjectMapper om;
  @Autowired private SupabaseConf supabaseConf;
  @Autowired private UserRepository userRepository;

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  void should_process_user_created_event_and_return_success_response() {
    var payload = buildUserCreatedPayload("RESEARCHER");
    var signature = computeSignature(payload);

    var response = supabaseAuthWebhookService.handleAuthWebhook(payload, signature);

    assertEquals("Webhook processed successfully", response.get("message"));
    assertEquals(TEST_SUPABASE_USER_ID, response.get("userId"));

    var savedUser = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID);
    assertTrue(savedUser.isPresent());
    assertInstanceOf(JResearcher.class, savedUser.get());
  }

  @Test
  void should_create_researcher_when_user_created_event() {
    var payload = buildUserCreatedPayload("RESEARCHER");
    var signature = computeSignature(payload);

    supabaseAuthWebhookService.handleAuthWebhook(payload, signature);

    var savedUser = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID);
    assertTrue(savedUser.isPresent());
    assertInstanceOf(JResearcher.class, savedUser.get());
    assertEquals(UserType.RESEARCHER, savedUser.get().getUserType());
  }

  @Test
  void should_create_seller_when_user_created_event_with_seller_type() {
    var payload = buildUserCreatedPayload("SELLER");
    var signature = computeSignature(payload);

    supabaseAuthWebhookService.handleAuthWebhook(payload, signature);

    var savedUser = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID);
    assertTrue(savedUser.isPresent());
    assertInstanceOf(JSeller.class, savedUser.get());
  }

  @Test
  void should_create_manager_when_user_created_event_with_manager_type() {
    var payload = buildUserCreatedPayload("MANAGER");
    var signature = computeSignature(payload);

    supabaseAuthWebhookService.handleAuthWebhook(payload, signature);

    var savedUser = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID);
    assertTrue(savedUser.isPresent());
    assertInstanceOf(JManager.class, savedUser.get());
  }

  @Test
  void should_update_user_when_user_updated_event() {
    createTestUser();

    var updatedName = "Jane Doe Updated";
    var payload = buildUserUpdatedPayload(updatedName);
    var signature = computeSignature(payload);

    supabaseAuthWebhookService.handleAuthWebhook(payload, signature);

    var updatedUser = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID);
    assertTrue(updatedUser.isPresent());
    assertEquals(updatedName, updatedUser.get().getName());
  }

  @Test
  void should_soft_delete_user_when_user_deleted_event() {
    createTestUser();

    var payload = buildUserDeletedPayload();
    var signature = computeSignature(payload);

    supabaseAuthWebhookService.handleAuthWebhook(payload, signature);

    var deletedUser = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID);
    assertTrue(deletedUser.isPresent());
    assertEquals(UserStatus.DISABLED, deletedUser.get().getStatus());
  }

  @Test
  void should_throw_exception_when_signature_is_null() {
    var payload = buildUserCreatedPayload("RESEARCHER");

    assertThrows(
        SecurityException.class, () -> supabaseAuthWebhookService.handleAuthWebhook(payload, null));
  }

  @Test
  void should_throw_exception_when_signature_is_blank() {
    var payload = buildUserCreatedPayload("RESEARCHER");

    assertThrows(
        SecurityException.class, () -> supabaseAuthWebhookService.handleAuthWebhook(payload, ""));
  }

  @Test
  void should_throw_exception_when_signature_is_invalid() {
    var payload = buildUserCreatedPayload("RESEARCHER");
    var invalidSignature = "invalid-signature";

    assertThrows(
        SecurityException.class,
        () -> supabaseAuthWebhookService.handleAuthWebhook(payload, invalidSignature));
  }

  @Test
  void should_throw_exception_when_payload_is_malformed() {
    var malformedPayload = "{invalid json";
    var signature = computeSignature(malformedPayload);

    assertThrows(
        IllegalArgumentException.class,
        () -> supabaseAuthWebhookService.handleAuthWebhook(malformedPayload, signature));
  }

  @Test
  void should_throw_exception_when_event_type_is_unknown() {
    var payload = buildWebhookPayload("user.unknown_event", buildUserMetadata("RESEARCHER"));
    var signature = computeSignature(payload);

    assertThrows(
        IllegalArgumentException.class,
        () -> supabaseAuthWebhookService.handleAuthWebhook(payload, signature));
  }

  @Test
  void should_handle_idempotent_webhook_calls() {
    var payload = buildUserCreatedPayload("RESEARCHER");
    var signature = computeSignature(payload);

    supabaseAuthWebhookService.handleAuthWebhook(payload, signature);
    var userCountAfterFirst = userRepository.count();
    assertEquals(1, userCountAfterFirst);

    supabaseAuthWebhookService.handleAuthWebhook(payload, signature);
    var userCountAfterSecond = userRepository.count();
    assertEquals(1, userCountAfterSecond);
  }

  @SneakyThrows
  private String buildUserCreatedPayload(String userType) {
    return buildWebhookPayload("user.created", buildUserMetadata(userType));
  }

  @SneakyThrows
  private String buildUserUpdatedPayload(String updatedName) {
    var metadata = buildUserMetadata("RESEARCHER");
    metadata.put("full_name", updatedName);
    return buildWebhookPayload("user.updated", metadata);
  }

  @SneakyThrows
  private String buildUserDeletedPayload() {
    return buildWebhookPayload("user.deleted", buildUserMetadata("RESEARCHER"));
  }

  @SneakyThrows
  private String buildWebhookPayload(String event, Map<String, Object> metadata) {
    var webhook = new HashMap<String, Object>();
    webhook.put("event", event);
    webhook.put("created_at", "2024-01-01T00:00:00Z");

    var user = new HashMap<String, Object>();
    user.put("id", TEST_SUPABASE_USER_ID);
    user.put("email", TEST_EMAIL);
    user.put("phone", TEST_PHONE);
    user.put("user_metadata", metadata);
    user.put("app_metadata", null);
    user.put("created_at", "2024-01-01T00:00:00Z");
    user.put("updated_at", "2024-01-02T00:00:00Z");
    user.put("deleted_at", null);

    webhook.put("user", user);

    return om.writeValueAsString(webhook);
  }

  private Map<String, Object> buildUserMetadata(String userType) {
    var metadata = new HashMap<String, Object>();
    metadata.put("user_type", userType);
    metadata.put("full_name", TEST_NAME);
    return metadata;
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

  private void createTestUser() {
    var user =
        JResearcher.builder()
            .id("test-user-id")
            .supabaseUserId(TEST_SUPABASE_USER_ID)
            .phoneNumber("+261 33 22 555 55")
            .profileImgUrl("")
            .location(vom.map(Location.getDefault()))
            .name(TEST_NAME)
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build();
    userRepository.save(user);
  }
}
