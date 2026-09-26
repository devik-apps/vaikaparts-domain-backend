package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.PartCategory.BATTERY;
import static com.devikapps.vaikaparts.model.classifier.PartCategory.ENGINE_PART;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.user.ProfileRecord;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.user.SupabaseWebhook;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

class UserMetadataCategoriesTest {
  private final UserRepository users = mock(UserRepository.class);
  private final UserCreationService creation =
      new UserCreationService(users, mock(ValueObjectMapper.class));
  private final UserSyncService sync = new UserSyncService(creation, users);

  private ProfileRecord profile(Map<String, Object> metadata) {
    return new ProfileRecord(
        "profile", "alice@example.com", null, null, null,
        metadata, Map.of("user_type", "SELLER"), null, null, null);
  }

  private SupabaseWebhook webhook(ProfileRecord profile) {
    return new SupabaseWebhook("UPDATE", "users", "auth", profile, null);
  }

  @Test
  void creates_seller_from_real_json_metadata() throws Exception {
    var profile = new ObjectMapper().readValue("""
        {
          "id": "profile", "email": "alice@example.com",
          "raw_app_meta_data": {"user_type": "SELLER"},
          "raw_user_meta_data": {
            "name": "Alice", "category_list": ["ENGINE_PART", "BATTERY", "ENGINE_PART"],
            "handle_all_category": false, "is_deliverying": true
          }
        }
        """, ProfileRecord.class);
    sync.handleUserCreated(webhook(profile));
    var capture = ArgumentCaptor.forClass(JUser.class);
    verify(users).saveAndFlush(capture.capture());
    var seller = (JSeller) capture.getValue();
    assertEquals(List.of(ENGINE_PART, BATTERY), seller.getCategoryList());
    assertFalse(seller.getHandleAllCategory());
    assertTrue(seller.getIsDeliverying());
    assertEquals("Alice", seller.getName());
    assertEquals("alice@example.com", seller.getEmail());
  }

  @ParameterizedTest
  @EnumSource(UserType.class)
  void preserves_name_and_email_for_every_user_type(UserType type) {
    creation.createUserIfAbsent(profile(Map.of("name", "Alice")), type);
    var capture = ArgumentCaptor.forClass(JUser.class);
    verify(users).saveAndFlush(capture.capture());
    assertEquals("Alice", capture.getValue().getName());
    assertEquals("alice@example.com", capture.getValue().getEmail());
    if (capture.getValue() instanceof JSeller seller) {
      assertTrue(seller.getHandleAllCategory());
      assertTrue(seller.getCategoryList().isEmpty());
      assertFalse(seller.getIsDeliverying());
    }
  }

  @Test
  void syncs_categories_and_boolean_on_update() {
    var seller = JSeller.builder().id("seller").build();
    when(users.findBySupabaseUserId("profile")).thenReturn(Optional.of(seller));
    sync.handleUserUpdated(webhook(profile(Map.of(
        "category_list", List.of("BATTERY"), "handle_all_category", false,
        "is_deliverying", true))));
    assertEquals(List.of(BATTERY), seller.getCategoryList());
    assertFalse(seller.getHandleAllCategory());
    assertTrue(seller.getIsDeliverying());
    verify(users).save(seller);
  }

  @Test
  void missing_metadata_preserves_existing_preferences_but_empty_list_clears_them() {
    var seller = JSeller.builder().categoryList(List.of(BATTERY)).handleAllCategory(false)
        .isDeliverying(true).build();
    creation.updateUserFields(seller, profile(null));
    assertEquals(List.of(BATTERY), seller.getCategoryList());
    assertFalse(seller.getHandleAllCategory());
    assertTrue(seller.getIsDeliverying());
    creation.updateUserFields(seller, profile(Map.of("category_list", List.of())));
    assertTrue(seller.getCategoryList().isEmpty());
    assertFalse(seller.getHandleAllCategory());
    assertTrue(seller.getIsDeliverying());
    creation.updateUserFields(seller, profile(Map.of("handle_all_category", true)));
    assertTrue(seller.getHandleAllCategory());
    creation.updateUserFields(seller, profile(Map.of("is_deliverying", "false")));
    assertTrue(seller.getIsDeliverying());
    creation.updateUserFields(seller, profile(Map.of("is_deliverying", false)));
    assertFalse(seller.getIsDeliverying());
  }

  @Test
  void duplicate_insert_updates_existing_seller() {
    var seller = JSeller.builder().build();
    when(users.findBySupabaseUserId("profile")).thenReturn(Optional.of(seller));
    creation.createUserIfAbsent(profile(Map.of(
        "category_list", List.of("ENGINE_PART"), "handle_all_category", false)), UserType.SELLER);
    assertEquals(List.of(ENGINE_PART), seller.getCategoryList());
    assertFalse(seller.getHandleAllCategory());
    verify(users, never()).saveAndFlush(any());
  }

  @Test
  void ignores_invalid_categories_without_losing_valid_entries() {
    var seller = JSeller.builder().build();
    creation.updateUserFields(seller, profile(Map.of(
        "category_list", List.of("UNKNOWN", 42, "BATTERY"))));
    assertEquals(List.of(BATTERY), seller.getCategoryList());
  }
}
