package com.devikapps.vaikaparts.service;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.user.ProfileRecord;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.user.SupabaseWebhook;
import com.devikapps.vaikaparts.model.classifier.ManagerRole;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import java.time.LocalDateTime;
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
  private static final String TEST_PROFILE_IMG_URL = "https://example.com/profile.jpg";
  private static final String TEST_GARAGE_NAME = "Joe's Garage";
  private static final LocalDateTime TEST_CREATED_AT = now();
  private static final LocalDateTime TEST_UPDATED_AT = now();

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

    var savedUser = findUserBySupabaseId();
    assertNotNull(savedUser);
    assertInstanceOf(JResearcher.class, savedUser);
    assertEquals(UserType.RESEARCHER, savedUser.getUserType());
    assertEquals(UserStatus.ENABLED, savedUser.getStatus());
    assertEquals(TEST_SUPABASE_USER_ID, savedUser.getSupabaseUserId());
    assertEquals(TEST_NAME, savedUser.getName());
    assertEquals(TEST_PHONE, savedUser.getPhoneNumber());
    assertEquals(TEST_PROFILE_IMG_URL, savedUser.getProfileImgKey());
  }

  @Test
  void should_create_researcher_with_location_from_metadata() {
    var metadata = buildResearcherMetadata();
    metadata.put("location", buildLocationMap());
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, metadata);

    userSyncService.handleUserCreated(webhook);

    var savedUser = (JResearcher) findUserBySupabaseId();
    assertNotNull(savedUser.getLocation());
    assertEquals("Test Address", savedUser.getLocation().getAddress());
  }

  @Test
  void should_create_seller_when_user_created_event_with_seller_type() {
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, buildSellerMetadata());

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId();
    assertNotNull(savedUser);
    assertInstanceOf(JSeller.class, savedUser);
    assertEquals(UserType.SELLER, savedUser.getUserType());
    assertEquals(TEST_GARAGE_NAME, ((JSeller) savedUser).getGarageName());
  }

  @Test
  void should_create_seller_with_location_and_latlon_from_metadata() {
    var metadata = buildSellerMetadata();
    metadata.put("location", buildLocationMap());
    metadata.put("lat_lon", buildLatLonMap());
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, metadata);

    userSyncService.handleUserCreated(webhook);

    var savedUser = (JSeller) findUserBySupabaseId();
    assertNotNull(savedUser.getLocation());
    assertNotNull(savedUser.getLatLon());
    assertEquals(-18.8792, savedUser.getLatLon().getLatitude());
    assertEquals(47.5079, savedUser.getLatLon().getLongitude());
    assertEquals(TEST_GARAGE_NAME, savedUser.getGarageName());
  }

  @Test
  void should_create_manager_when_user_created_event_with_manager_type() {
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, buildManagerMetadata());

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId();
    assertNotNull(savedUser);
    assertInstanceOf(JManager.class, savedUser);
    assertEquals(UserType.MANAGER, savedUser.getUserType());
    assertEquals(ManagerRole.ADMIN, ((JManager) savedUser).getManagerRole());
  }

  @Test
  void should_default_to_researcher_when_user_type_not_specified() {
    var metadata = new HashMap<String, Object>();
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, metadata);

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId();
    assertNotNull(savedUser);
    assertInstanceOf(JResearcher.class, savedUser);
    assertEquals(UserType.RESEARCHER, savedUser.getUserType());
    assertEquals(TEST_SUPABASE_USER_ID, savedUser.getSupabaseUserId());
  }

  @Test
  void should_default_to_researcher_when_invalid_user_type_specified() {
    var metadata = buildResearcherMetadata();
    metadata.put("user_type", "INVALID_TYPE");
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, metadata);

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId();
    assertNotNull(savedUser);
    assertInstanceOf(JResearcher.class, savedUser);
  }

  @Test
  void should_use_email_username_when_name_not_provided() {
    var metadata = new HashMap<String, Object>();
    metadata.put("user_type", "RESEARCHER");
    var webhook = buildWebhookWithCustomName(metadata, null);

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId();
    assertNotNull(savedUser);
    assertEquals("test", savedUser.getName());
  }

  @Test
  void should_skip_creation_when_user_already_exists() {
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, buildResearcherMetadata());
    userSyncService.handleUserCreated(webhook);

    var initialUser = findUserBySupabaseId();
    var initialUserId = initialUser.getId();

    userSyncService.handleUserCreated(webhook);

    var userCount = userRepository.count();
    assertEquals(1, userCount);

    var finalUser = findUserBySupabaseId();
    assertEquals(initialUserId, finalUser.getId());
  }

  @Test
  void should_update_user_fields_when_user_updated_event_received() {
    var createWebhook = buildWebhook(TEST_SUPABASE_USER_ID, buildResearcherMetadata());
    userSyncService.handleUserCreated(createWebhook);

    var updatedName = "Jane Doe Updated";
    var updateWebhook = buildWebhookWithCustomName(buildResearcherMetadata(), updatedName);

    userSyncService.handleUserUpdated(updateWebhook);

    var updatedUser = findUserBySupabaseId();
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

    var updatedUser = (JSeller) findUserBySupabaseId();
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

    var deleteWebhook = buildDeleteWebhook(TEST_SUPABASE_USER_ID, buildResearcherMetadata());

    userSyncService.handleUserDeleted(deleteWebhook);

    var deletedUser = findUserBySupabaseId();
    assertEquals(UserStatus.DISABLED, deletedUser.getStatus());
    assertNotNull(deletedUser.getUpdatedAt());
  }

  @Test
  void should_throw_exception_when_deleting_non_existent_user() {
    var webhook = buildDeleteWebhook("non-existent-id", buildResearcherMetadata());

    assertThrows(IllegalStateException.class, () -> userSyncService.handleUserDeleted(webhook));
  }

  @Test
  void should_handle_null_metadata_gracefully() {
    var webhook = buildWebhook(TEST_SUPABASE_USER_ID, null);

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId();
    assertNotNull(savedUser);
    assertEquals(TEST_NAME, savedUser.getName());
  }

  @Test
  void should_prefer_name_field_over_metadata_name() {
    var metadata = new HashMap<String, Object>();
    metadata.put("user_type", "RESEARCHER");
    metadata.put("name", "Metadata Name");
    var webhook = buildWebhookWithCustomName(metadata, "Profile Name");

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId();
    assertEquals("Profile Name", savedUser.getName());
    assertEquals(UserType.RESEARCHER, savedUser.getUserType());
  }

  @Test
  void should_use_metadata_name_when_profile_name_is_blank() {
    var metadata = new HashMap<String, Object>();
    metadata.put("user_type", "RESEARCHER");
    metadata.put("name", "Metadata Name");
    var webhook = buildWebhookWithCustomName(metadata, "");

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId();
    assertEquals("Metadata Name", savedUser.getName());
  }

  @Test
  void should_use_profile_phone_number() {
    var webhook = buildWebhookWithCustomPhone(buildResearcherMetadata());

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId();
    assertEquals(TEST_PHONE, savedUser.getPhoneNumber());
  }

  @Test
  void should_use_profile_img_url() {
    var metadata = buildResearcherMetadata();
    var webhook = buildWebhookWithCustomProfileImg(metadata);

    userSyncService.handleUserCreated(webhook);

    var savedUser = findUserBySupabaseId();
    assertEquals(TEST_PROFILE_IMG_URL, savedUser.getProfileImgKey());
  }

  private Map<String, Object> buildResearcherMetadata() {
    var metadata = new HashMap<String, Object>();
    metadata.put("user_type", "RESEARCHER");
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

  private Map<String, Object> buildLocationMap() {
    var location = new HashMap<String, Object>();
    location.put("city", "ANTANANARIVO");
    location.put("region", "ANALAMANGA");
    location.put("address", "Test Address");
    return location;
  }

  private Map<String, Object> buildLatLonMap() {
    var latLon = new HashMap<String, Object>();
    latLon.put("lat", -18.8792);
    latLon.put("lon", 47.5079);
    return latLon;
  }

  private SupabaseWebhook buildWebhook(String profileId, Map<String, Object> metadata) {
    return buildWebhookWithAllFields(profileId, metadata, TEST_NAME);
  }

  private SupabaseWebhook buildWebhookWithCustomName(Map<String, Object> metadata, String name) {
    return buildWebhookWithAllFields(UserSyncServiceIT.TEST_SUPABASE_USER_ID, metadata, name);
  }

  private SupabaseWebhook buildWebhookWithCustomPhone(Map<String, Object> metadata) {
    return buildWebhookWithAllFields(UserSyncServiceIT.TEST_SUPABASE_USER_ID, metadata, TEST_NAME);
  }

  private SupabaseWebhook buildWebhookWithCustomProfileImg(Map<String, Object> metadata) {
    return buildWebhookWithAllFields(UserSyncServiceIT.TEST_SUPABASE_USER_ID, metadata, TEST_NAME);
  }

  private SupabaseWebhook buildWebhookWithAllFields(
      String profileId, Map<String, Object> metadata, String name) {
    var profileRecord =
        new ProfileRecord(
            profileId,
            TEST_EMAIL,
            UserSyncServiceIT.TEST_PHONE,
            name,
            UserSyncServiceIT.TEST_PROFILE_IMG_URL,
            metadata != null ? metadata : Map.of(),
            Map.of(),
            TEST_CREATED_AT,
            TEST_UPDATED_AT,
            null);
    return new SupabaseWebhook("INSERT", "profiles", "public", profileRecord, null);
  }

  private SupabaseWebhook buildDeleteWebhook(String profileId, Map<String, Object> metadata) {
    var profileRecord =
        new ProfileRecord(
            profileId,
            TEST_EMAIL,
            TEST_PHONE,
            TEST_NAME,
            TEST_PROFILE_IMG_URL,
            metadata != null ? metadata : Map.of(),
            Map.of(),
            TEST_CREATED_AT,
            TEST_UPDATED_AT,
            null);
    return new SupabaseWebhook("DELETE", "profiles", "public", null, profileRecord);
  }

  private JUser findUserBySupabaseId() {
    Optional<JUser> userOpt =
        userRepository.findBySupabaseUserId(UserSyncServiceIT.TEST_SUPABASE_USER_ID);
    assertTrue(
        userOpt.isPresent(),
        format("User should exist with profileId: %s", UserSyncServiceIT.TEST_SUPABASE_USER_ID));
    return userOpt.get();
  }
}
