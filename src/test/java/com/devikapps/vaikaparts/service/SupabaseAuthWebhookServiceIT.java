package com.devikapps.vaikaparts.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.config.SupabaseConf;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SupabaseAuthWebhookServiceIT extends FacadeIT {

  private static final String TEST_SUPABASE_USER_ID = "550e8400-e29b-41d4-a716-446655440000";
  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_PHONE = "+1234567890";
  private static final String TEST_NAME = "John Doe";

  @Autowired private ValueObjectMapper vom;
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

    var response =
        supabaseAuthWebhookService.handleAuthWebhook(payload, supabaseConf.getWebhookSecret());

    assertEquals("Webhook processed successfully", response.get("message"));
    assertEquals(TEST_SUPABASE_USER_ID, response.get("userId"));
    assertEquals("INSERT", response.get("eventType"));

    var savedUser = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID);
    assertTrue(savedUser.isPresent());
    assertInstanceOf(JResearcher.class, savedUser.get());
  }

  @Test
  void should_create_researcher_when_user_created_event() {
    var payload = buildUserCreatedPayload("RESEARCHER");

    supabaseAuthWebhookService.handleAuthWebhook(payload, supabaseConf.getWebhookSecret());

    var savedUser = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID);
    assertTrue(savedUser.isPresent());
    assertInstanceOf(JResearcher.class, savedUser.get());
    assertEquals(UserType.RESEARCHER, savedUser.get().getUserType());
  }

  @Test
  void should_create_seller_when_user_created_event_with_seller_type() {
    var payload = buildUserCreatedPayload("SELLER");

    supabaseAuthWebhookService.handleAuthWebhook(payload, supabaseConf.getWebhookSecret());

    var savedUser = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID);
    assertTrue(savedUser.isPresent());
    assertInstanceOf(JSeller.class, savedUser.get());
  }

  @Test
  void should_create_manager_when_user_created_event_with_manager_type() {
    var payload = buildUserCreatedPayload("MANAGER");

    supabaseAuthWebhookService.handleAuthWebhook(payload, supabaseConf.getWebhookSecret());

    var savedUser = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID);
    assertTrue(savedUser.isPresent());
    assertInstanceOf(JManager.class, savedUser.get());
  }

  @Test
  void should_update_user_when_user_updated_event() {
    createTestUser();

    var updatedName = "Jane Doe Updated";
    var payload = buildUserUpdatedPayload(updatedName);

    supabaseAuthWebhookService.handleAuthWebhook(payload, supabaseConf.getWebhookSecret());

    var updatedUser = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID);
    assertTrue(updatedUser.isPresent());
    assertEquals(updatedName, updatedUser.get().getName());
  }

  @Test
  void should_soft_delete_user_when_user_deleted_event() {
    createTestUser();

    var payload = buildUserDeletedPayload();

    supabaseAuthWebhookService.handleAuthWebhook(payload, supabaseConf.getWebhookSecret());

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

    assertThrows(
        IllegalArgumentException.class,
        () ->
            supabaseAuthWebhookService.handleAuthWebhook(
                malformedPayload, supabaseConf.getWebhookSecret()));
  }

  @Test
  void should_throw_exception_when_event_type_is_unknown() {
    var payload = buildWebhookPayload("UNKNOWN_EVENT", buildUserMetadata("RESEARCHER"), TEST_NAME);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            supabaseAuthWebhookService.handleAuthWebhook(payload, supabaseConf.getWebhookSecret()));
  }

  @Test
  void should_handle_idempotent_webhook_calls() {
    var payload = buildUserCreatedPayload("RESEARCHER");

    supabaseAuthWebhookService.handleAuthWebhook(payload, supabaseConf.getWebhookSecret());
    var userCountAfterFirst = userRepository.count();
    assertEquals(1, userCountAfterFirst);

    supabaseAuthWebhookService.handleAuthWebhook(payload, supabaseConf.getWebhookSecret());
    var userCountAfterSecond = userRepository.count();
    assertEquals(1, userCountAfterSecond);
  }

  @SneakyThrows
  private String buildUserCreatedPayload(String userType) {
    return buildWebhookPayload("INSERT", buildUserMetadata(userType), TEST_NAME);
  }

  @SneakyThrows
  private String buildUserUpdatedPayload(String updatedName) {
    return buildWebhookPayload("UPDATE", buildUserMetadata("RESEARCHER"), updatedName);
  }

  @SneakyThrows
  private String buildUserDeletedPayload() {
    var webhook = new HashMap<String, Object>();
    webhook.put("type", "DELETE");
    webhook.put("table", "profiles");
    webhook.put("schema", "public");
    webhook.put("record", null);

    var oldRecord = buildProfileRecord(buildUserMetadata("RESEARCHER"), TEST_NAME);
    webhook.put("old_record", oldRecord);

    return om.writeValueAsString(webhook);
  }

  @SneakyThrows
  private String buildWebhookPayload(String eventType, Map<String, Object> metadata, String name) {
    var webhook = new HashMap<String, Object>();
    webhook.put("type", eventType);
    webhook.put("table", "profiles");
    webhook.put("schema", "public");

    var record = buildProfileRecord(metadata, name);
    webhook.put("record", record);
    webhook.put("old_record", null);

    return om.writeValueAsString(webhook);
  }

  private Map<String, Object> buildProfileRecord(Map<String, Object> metadata, String name) {
    var record = new HashMap<String, Object>();
    record.put("id", TEST_SUPABASE_USER_ID);
    record.put("email", TEST_EMAIL);
    record.put("phone_number", TEST_PHONE);
    record.put("name", name);
    record.put("profile_img_url", "");
    record.put("user_metadata", metadata);
    record.put("app_metadata", Map.of());
    record.put("created_at", "2024-01-01T00:00:00Z");
    record.put("updated_at", "2024-01-02T00:00:00Z");
    record.put("deleted_at", null);
    return record;
  }

  private Map<String, Object> buildUserMetadata(String userType) {
    var metadata = new HashMap<String, Object>();
    metadata.put("user_type", userType);
    if ("SELLER".equals(userType)) {
      metadata.put("garage_name", "Test Garage");
    } else if ("MANAGER".equals(userType)) {
      metadata.put("manager_role", "ADMIN");
    }
    return metadata;
  }

  private void createTestUser() {
    var user =
        JResearcher.builder()
            .id("test-user-id")
            .supabaseUserId(TEST_SUPABASE_USER_ID)
            .phoneNumber(TEST_PHONE)
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .name(TEST_NAME)
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build();
    userRepository.save(user);
  }
}
