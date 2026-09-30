package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.Arrondissement.FIFTH;
import static com.devikapps.vaikaparts.model.classifier.Arrondissement.FIRST;
import static com.devikapps.vaikaparts.model.classifier.Arrondissement.SIXTH;
import static com.devikapps.vaikaparts.model.classifier.Arrondissement.THIRD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.user.ProfileRecord;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.user.SupabaseWebhook;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.classifier.Arrondissement;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserMetadataArrondissementTest {
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
  void creates_seller_with_arrondissement_from_json_metadata() throws Exception {
    var profile = new ObjectMapper().readValue("""
        {
          "id": "profile", "email": "alice@example.com",
          "raw_app_meta_data": {"user_type": "SELLER"},
          "raw_user_meta_data": {"name": "Alice", "arrondissement": "3"}
        }
        """, ProfileRecord.class);
    sync.handleUserCreated(webhook(profile));
    var capture = ArgumentCaptor.forClass(JUser.class);
    verify(users).saveAndFlush(capture.capture());
    var seller = (JSeller) capture.getValue();
    assertEquals(THIRD, seller.getArrondissement());
  }

  @Test
  void accepts_numeric_and_enum_and_french_formats() {
    creation.createUserIfAbsent(profile(Map.of("arrondissement", 1)), UserType.SELLER);
    assertEquals(FIRST, capturedSeller().getArrondissement());

    creation.createUserIfAbsent(profile(Map.of("arrondissement", "5e")), UserType.SELLER);
    assertEquals(FIFTH, capturedSeller().getArrondissement());

    creation.createUserIfAbsent(profile(Map.of("arrondissement", "SIXTH")), UserType.SELLER);
    assertEquals(SIXTH, capturedSeller().getArrondissement());
  }

  @Test
  void syncs_arrondissement_on_update() {
    var seller = JSeller.builder().id("seller").build();
    when(users.findBySupabaseUserId("profile")).thenReturn(Optional.of(seller));
    sync.handleUserUpdated(webhook(profile(Map.of("arrondissement", "2"))));
    assertEquals(Arrondissement.SECOND, seller.getArrondissement());
    verify(users).save(seller);
  }

  @Test
  void missing_or_invalid_metadata_preserves_existing_value() {
    var seller = JSeller.builder().arrondissement(SIXTH).build();
    creation.updateUserFields(seller, profile(null));
    assertEquals(SIXTH, seller.getArrondissement());

    creation.updateUserFields(seller, profile(Map.of("arrondissement", "SEVENTH")));
    assertEquals(SIXTH, seller.getArrondissement());

    creation.updateUserFields(seller, profile(Map.of("arrondissement", 99)));
    assertEquals(SIXTH, seller.getArrondissement());

    creation.updateUserFields(seller, profile(Map.of("arrondissement", true)));
    assertEquals(SIXTH, seller.getArrondissement());
  }

  @Test
  void defaults_to_null_when_absent_on_creation() {
    creation.createUserIfAbsent(profile(Map.of("name", "Alice")), UserType.SELLER);
    var seller = capturedSeller();
    assertNull(seller.getArrondissement());
    assertFalse(seller.getIsDeliverying());
  }

  private JSeller capturedSeller() {
    var capture = ArgumentCaptor.forClass(JUser.class);
    verify(users, atLeastOnce()).saveAndFlush(capture.capture());
    var values = capture.getAllValues();
    var user = values.get(values.size() - 1);
    if (!(user instanceof JSeller seller)) throw new IllegalStateException("Not a seller");
    return seller;
  }
}
