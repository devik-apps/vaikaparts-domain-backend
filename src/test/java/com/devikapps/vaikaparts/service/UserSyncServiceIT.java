package com.devikapps.vaikaparts.service;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.SupabaseAuthWebhook;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.UserData;
import com.devikapps.vaikaparts.model.classifier.ManagerRole;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserSyncServiceIT extends FacadeIT {

  private static final String TEST_SUPABASE_USER_ID = "supabase-user-123";
  private static final String TEST_EMAIL = "test@example.com";
  private static final String TEST_PHONE = "+1234567890";
  private static final String TEST_NAME = "John Doe";
  private static final String TEST_AVATAR_URL = "https://example.com/avatar.jpg";
  private static final String TEST_GARAGE_NAME = "Joe's Garage";
  private static final Instant TEST_CREATED_AT = Instant.parse("2024-01-01T00:00:00Z");
  private static final Instant TEST_UPDATED_AT = Instant.parse("2024-01-02T00:00:00Z");

  @Autowired private UserSyncService userSyncService;
  @Autowired private UserRepository userRepository;

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  void should_create_researcher_when_user_created_event_received() {
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, buildResearcherMetadata());

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertNotNull(savedUser);
    assertInstanceOf(JResearcher.class, savedUser);
    assertEquals(UserType.RESEARCHER, savedUser.getUserType());
    assertEquals(UserStatus.ENABLED, savedUser.getStatus());
    assertEquals(TEST_SUPABASE_USER_ID, savedUser.getSupabaseUserId());
    assertEquals(TEST_NAME, savedUser.getName());
    assertEquals(TEST_PHONE, savedUser.getPhoneNumber());
    assertEquals(TEST_AVATAR_URL, savedUser.getProfileImgUrl());
  }

  @Test
  void should_create_seller_when_user_created_event_with_seller_type() {
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, buildSellerMetadata());

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertNotNull(savedUser);
    assertInstanceOf(JSeller.class, savedUser);
    assertEquals(UserType.SELLER, savedUser.getUserType());
    assertEquals(TEST_GARAGE_NAME, ((JSeller) savedUser).getGarageName());
  }

  @Test
  void should_create_manager_when_user_created_event_with_manager_type() {
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, buildManagerMetadata());

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertNotNull(savedUser);
    assertInstanceOf(JManager.class, savedUser);
    assertEquals(UserType.MANAGER, savedUser.getUserType());
    assertEquals(ManagerRole.ADMIN, ((JManager) savedUser).getManagerRole());
  }

  @Test
  void should_default_to_researcher_when_user_type_not_specified() {
    var metadata = new HashMap<String, Object>();
    metadata.put("full_name", TEST_NAME);
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, metadata);

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertNotNull(savedUser);
    assertInstanceOf(JResearcher.class, savedUser);
    assertEquals(UserType.RESEARCHER, savedUser.getUserType());
  }

  @Test
  void should_default_to_researcher_when_invalid_user_type_specified() {
    var metadata = buildResearcherMetadata();
    metadata.put("user_type", "INVALID_TYPE");
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, metadata);

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertNotNull(savedUser);
    assertInstanceOf(JResearcher.class, savedUser);
  }

  @Test
  void should_use_email_username_when_name_not_provided() {
    var metadata = new HashMap<String, Object>();
    metadata.put("user_type", "RESEARCHER");
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, metadata);

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertNotNull(savedUser);
    assertEquals("test", savedUser.getName());
  }

  @Test
  void should_skip_creation_when_user_already_exists() {
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, buildResearcherMetadata());
    userSyncService.handleUserCreated(webhook);

    var initialUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    var initialUserId = initialUser.getId();

    userSyncService.handleUserCreated(webhook);

    var userCount = userRepository.count();
    assertEquals(1, userCount);

    var finalUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertEquals(initialUserId, finalUser.getId());
  }

  @Test
  void should_update_user_fields_when_user_updated_event_received() {
    var createWebhook = buildWebhook(TEST_SUPABASE_USER_ID, buildResearcherMetadata());
    userSyncService.handleUserCreated(createWebhook);

    var updatedName = "Jane Doe Updated";
    var updatedMetadata = buildResearcherMetadata();
    updatedMetadata.put("full_name", updatedName);
    var updateWebhook = buildWebhook(TEST_SUPABASE_USER_ID, updatedMetadata);

    userSyncService.handleUserUpdated(updateWebhook);

    var updatedUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertEquals(updatedName, updatedUser.getName());
  }

  @Test
  void should_update_seller_garage_name_when_seller_updated() {
    var createWebhook = buildWebhook(TEST_SUPABASE_USER_ID, buildSellerMetadata());
    userSyncService.handleUserCreated(createWebhook);

    var updatedGarageName = "Updated Garage";
    var updatedMetadata = buildSellerMetadata();
    updatedMetadata.put("garage_name", updatedGarageName);
    var updateWebhook = buildWebhook(TEST_SUPABASE_USER_ID, updatedMetadata);

    userSyncService.handleUserUpdated(updateWebhook);

    var updatedUser = (JSeller) findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertEquals(updatedGarageName, updatedUser.getGarageName());
  }

  @Test
  void should_throw_exception_when_updating_non_existent_user() {
    var webhook = buildWebhook("non-existent-id", buildResearcherMetadata());

    assertThrows(IllegalStateException.class, () -> userSyncService.handleUserUpdated(webhook));
  }

  @Test
  void should_soft_delete_user_when_user_deleted_event_received() {
    var createWebhook = buildWebhook(TEST_SUPABASE_USER_ID, buildResearcherMetadata());
    userSyncService.handleUserCreated(createWebhook);

    var deleteWebhook = buildWebhook(TEST_SUPABASE_USER_ID, buildResearcherMetadata());

    userSyncService.handleUserDeleted(deleteWebhook);

    var deletedUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertEquals(UserStatus.DISABLED, deletedUser.getStatus());
    assertNotNull(deletedUser.getUpdatedAt());
  }

  @Test
  void should_throw_exception_when_deleting_non_existent_user() {
    var webhook = buildWebhook("non-existent-id", buildResearcherMetadata());

    assertThrows(IllegalStateException.class, () -> userSyncService.handleUserDeleted(webhook));
  }

  @Test
  void should_handle_null_metadata_gracefully() {
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, null);

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertNotNull(savedUser);
    assertEquals("test", savedUser.getName());
  }

  @Test
  void should_prefer_full_name_over_name_field() {
    var metadata = new HashMap<String, Object>();
    metadata.put("user_type", "RESEARCHER");
    metadata.put("name", "Short Name");
    metadata.put("full_name", "Full Name Version");
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, metadata);

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertEquals("Full Name Version", savedUser.getName());
  }

  @Test
  void should_prefer_phone_field_over_metadata_phone() {
    var metadata = buildResearcherMetadata();
    metadata.put("phone", "+9999999999");
    var webhook = buildWebhookWithCustomPhone(TEST_SUPABASE_USER_ID, metadata, TEST_PHONE);

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertEquals(TEST_PHONE, savedUser.getPhoneNumber());
  }

  @Test
  void should_use_metadata_phone_when_phone_field_is_blank() {
    var metadataPhone = "+9999999999";
    var metadata = buildResearcherMetadata();
    metadata.put("phone", metadataPhone);
    var webhook = buildWebhookWithCustomPhone(TEST_SUPABASE_USER_ID, metadata, "");

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId(TEST_SUPABASE_USER_ID);
    assertEquals(metadataPhone, savedUser.getPhoneNumber());
  }

  @Test
  void should_support_both_avatar_url_and_profile_img_url() {
    var metadata1 = new HashMap<String, Object>();
    metadata1.put("user_type", "RESEARCHER");
    metadata1.put("avatar_url", TEST_AVATAR_URL);
    var webhook1 = buildWebhook("user-1", metadata1);

    userSyncService.handleUserCreated(webhook1);

    var user1 = findUserBySupabaseId("user-1");
    assertEquals(TEST_AVATAR_URL, user1.getProfileImgUrl());

    var profileImgUrl = "https://example.com/profile.jpg";
    var metadata2 = new HashMap<String, Object>();
    metadata2.put("user_type", "RESEARCHER");
    metadata2.put("profile_img_url", profileImgUrl);
    var webhook2 = buildWebhook("user-2", metadata2);

    userSyncService.handleUserCreated(webhook2);

    var user2 = findUserBySupabaseId("user-2");
    assertEquals(profileImgUrl, user2.getProfileImgUrl());
  }

  private Map<String, Object> buildResearcherMetadata() {
    var metadata = new HashMap<String, Object>();
    metadata.put("user_type", "RESEARCHER");
    metadata.put("name", TEST_NAME);
    metadata.put("avatar_url", TEST_AVATAR_URL);
    return metadata;
  }

  private Map<String, Object> buildSellerMetadata() {
    var metadata = buildResearcherMetadata();
    metadata.put("user_type", "SELLER");
    metadata.put("garage_name", TEST_GARAGE_NAME);
    return metadata;
  }

  private Map<String, Object> buildManagerMetadata() {
    var metadata = buildResearcherMetadata();
    metadata.put("user_type", "MANAGER");
    metadata.put("manager_role", "ADMIN");
    return metadata;
  }

  private SupabaseAuthWebhook buildWebhook(String supabaseUserId, Map<String, Object> metadata) {
    return buildWebhookWithCustomPhone(supabaseUserId, metadata, TEST_PHONE);
  }

  private SupabaseAuthWebhook buildWebhookWithCustomPhone(
      String supabaseUserId, Map<String, Object> metadata, String phone) {
    var userData =
        new UserData(
            supabaseUserId,
            TEST_EMAIL,
            phone,
            metadata,
            null,
            TEST_CREATED_AT,
            TEST_UPDATED_AT,
            null);
    return new SupabaseAuthWebhook("user.created", userData, TEST_CREATED_AT);
  }

  private JUser findUserBySupabaseId(String supabaseUserId) {
    Optional<JUser> userOpt = userRepository.findBySupabaseUserId(supabaseUserId);
    assertTrue(
        userOpt.isPresent(), format("User should exist with supabaseUserId: %s", supabaseUserId));
    return userOpt.get();
  }
}
