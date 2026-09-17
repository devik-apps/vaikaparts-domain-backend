package com.devikapps.vaikaparts.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.user.ProfileRecord;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.user.SupabaseWebhook;
import com.devikapps.vaikaparts.mapper.ImageUrlMapper;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.mapper.user.*;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.user.*;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class UserNotificationPreferencesTest {
  private final UserRepository repository = mock(UserRepository.class);
  private final UserCreationService creation =
      new UserCreationService(repository, mock(ValueObjectMapper.class));

  @Test
  void researcher_created_before_webhook_keeps_metadata_preferences() {
    var service = new UserService(repository, mock(ProfilePhotoService.class),
        mock(SellerMapper.class), mock(ResearcherMapper.class), mock(ManagerMapper.class),
        mock(com.devikapps.vaikaparts.service.util.Paginator.class), mock(ValueObjectMapper.class));
    var principal = new com.devikapps.vaikaparts.config.sec.AuthenticatedSupabaseUser(
        "profile", "alice@example.com", null,
        Map.of("email_notifications_enabled", true, "sms_notifications_enabled", true),
        Map.of("user_type", "RESEARCHER"));
    var previous = org.springframework.security.core.context.SecurityContextHolder.getContext();
    var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            principal, null, java.util.List.of()));
    org.springframework.security.core.context.SecurityContextHolder.setContext(context);
    when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    try {
      var researcher = service.getOrCreateCurrentResearcher();
      assertTrue(researcher.isEmailNotificationsEnabled());
      assertTrue(researcher.isSmsNotificationsEnabled());
    } finally {
      org.springframework.security.core.context.SecurityContextHolder.setContext(previous);
    }
  }

  private ProfileRecord profile(UserType type, Map<String, Object> metadata) {
    return new ProfileRecord("profile", "alice@example.com", null, "Alice", null,
        metadata, Map.of("user_type", type.name()), null, null, null);
  }

  private JUser create(UserType type, Map<String, Object> metadata) {
    creation.createUserIfAbsent(profile(type, metadata), type);
    var captured = ArgumentCaptor.forClass(JUser.class);
    verify(repository).saveAndFlush(captured.capture());
    return captured.getValue();
  }

  @ParameterizedTest
  @EnumSource(UserType.class)
  void defaults_to_in_app_only(UserType type) {
    var user = create(type, null);
    assertFalse(user.isEmailNotificationsEnabled());
    assertFalse(user.isSmsNotificationsEnabled());
  }

  @ParameterizedTest
  @EnumSource(UserType.class)
  void reads_real_json_booleans_and_updates_preferences(UserType type) throws Exception {
    Map<String, Object> metadata = new ObjectMapper().readValue(
        "{\"email_notifications_enabled\":true,\"sms_notifications_enabled\":true}", Map.class);
    var user = create(type, metadata);
    assertTrue(user.isEmailNotificationsEnabled());
    assertTrue(user.isSmsNotificationsEnabled());
    when(repository.findBySupabaseUserId("profile")).thenReturn(Optional.of(user));
    var sync = new UserSyncService(creation, repository);
    sync.handleUserUpdated(new SupabaseWebhook("UPDATE", "users", "auth",
        profile(type, Map.of("email_notifications_enabled", false)), null));
    assertFalse(user.isEmailNotificationsEnabled());
    assertTrue(user.isSmsNotificationsEnabled());
    creation.updateUserFields(user, profile(type, Map.of("sms_notifications_enabled", false)));
    assertFalse(user.isSmsNotificationsEnabled());
  }

  @ParameterizedTest
  @EnumSource(UserType.class)
  void missing_and_invalid_values_preserve_choices(UserType type) {
    var user = create(type, Map.of("email_notifications_enabled", true,
        "sms_notifications_enabled", true));
    creation.updateUserFields(user, profile(type, null));
    creation.updateUserFields(user, profile(type, Map.of()));
    var invalid = new HashMap<String, Object>();
    invalid.put("email_notifications_enabled", "false");
    invalid.put("sms_notifications_enabled", null);
    creation.updateUserFields(user, profile(type, invalid));
    assertTrue(user.isEmailNotificationsEnabled());
    assertTrue(user.isSmsNotificationsEnabled());
  }

  @ParameterizedTest
  @EnumSource(UserType.class)
  void maps_preferences_in_both_directions_for_all_profiles(UserType type) {
    var user = create(type, Map.of("email_notifications_enabled", true,
        "sms_notifications_enabled", true));
    User domain;
    JUser restored;
    switch (type) {
      case SELLER -> {
        var mapper = mapper(SellerMapper.class, true);
        domain = mapper.toSeller((JSeller) user);
        restored = mapper.toPersistence((Seller) domain);
      }
      case RESEARCHER -> {
        var mapper = mapper(ResearcherMapper.class, true);
        domain = mapper.toResearcher((JResearcher) user);
        restored = mapper.toPersistence((Researcher) domain);
      }
      case MANAGER -> {
        var mapper = mapper(ManagerMapper.class, false);
        domain = mapper.toManager((JManager) user);
        restored = mapper.toPersistence((Manager) domain);
      }
      default -> throw new AssertionError(type);
    }
    assertTrue(domain.isEmailNotificationsEnabled());
    assertTrue(domain.isSmsNotificationsEnabled());
    assertTrue(restored.isEmailNotificationsEnabled());
    assertTrue(restored.isSmsNotificationsEnabled());
  }

  private <T> T mapper(Class<T> type, boolean hasValueObjects) {
    var mapper = Mappers.getMapper(type);
    ReflectionTestUtils.setField(mapper, "imageUrlMapper", mock(ImageUrlMapper.class));
    if (hasValueObjects) {
      ReflectionTestUtils.setField(mapper, "valueObjectMapper", mock(ValueObjectMapper.class));
    }
    return mapper;
  }
}
