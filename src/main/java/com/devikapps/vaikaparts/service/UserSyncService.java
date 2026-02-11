package com.devikapps.vaikaparts.service;

import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.ofInstant;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.SupabaseAuthWebhook;
import com.devikapps.vaikaparts.mapper.user.ValueObjectMapper;
import com.devikapps.vaikaparts.model.LatLon;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.ManagerRole;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

  private static final String USER_TYPE_KEY = "user_type";
  private static final String FULL_NAME_KEY = "full_name";
  private static final String NAME_KEY = "name";
  private static final String PHONE_KEY = "phone";
  private static final String AVATAR_URL_KEY = "avatar_url";
  private static final String PROFILE_IMG_URL_KEY = "profile_img_url";
  private static final String GARAGE_NAME_KEY = "garage_name";
  private static final String MANAGER_ROLE_KEY = "manager_role";

  private final UserRepository userRepository;
  private final ValueObjectMapper vom;

  @Transactional
  public void handleUserCreated(SupabaseAuthWebhook webhook) {
    var supabaseUserId = webhook.user().id();
    log.info("Processing user.created event for Supabase user ID: {}", forJava(supabaseUserId));

    if (userRepository.existsBySupabaseUserId(supabaseUserId)) {
      log.warn(
          "User with Supabase ID {} already exists, skipping creation", forJava(supabaseUserId));
      return;
    }

    var userType = extractUserType(webhook.user().userMetadata());
    var newUser = createUserByType(webhook, userType);

    userRepository.save(newUser);
    log.info(
        "Successfully created {} with ID {} for Supabase user {}",
        userType,
        forJava(newUser.getId()),
        forJava(supabaseUserId));
  }

  @Transactional
  public void handleUserUpdated(SupabaseAuthWebhook webhook) {
    var supabaseUserId = webhook.user().id();
    log.info("Processing user.updated event for Supabase user ID: {}", forJava(supabaseUserId));

    var user = findUserBySupabaseId(supabaseUserId);
    updateUserFields(user, webhook);
    user.setUpdatedAt(convertToLocalDateTime(webhook.user().updatedAt()));

    userRepository.save(user);
    log.info(
        "Successfully updated user {} for Supabase user {}",
        forJava(user.getId()),
        forJava(supabaseUserId));
  }

  @Transactional
  public void handleUserDeleted(SupabaseAuthWebhook webhook) {
    var supabaseUserId = webhook.user().id();
    log.info("Processing user.deleted event for Supabase user ID: {}", forJava(supabaseUserId));

    var user = findUserBySupabaseId(supabaseUserId);
    user.setStatus(UserStatus.DISABLED);
    user.setUpdatedAt(now());

    userRepository.save(user);
    log.info(
        "Successfully soft-deleted user {} for Supabase user {}",
        forJava(user.getId()),
        forJava(supabaseUserId));
  }

  private JUser findUserBySupabaseId(String supabaseUserId) {
    return userRepository
        .findBySupabaseUserId(supabaseUserId)
        .orElseThrow(
            () -> {
              log.error("User not found for Supabase ID: {}", forJava(supabaseUserId));
              return new IllegalStateException(
                  "Cannot process non-existent user: " + supabaseUserId);
            });
  }

  private UserType extractUserType(Map<String, Object> userMetadata) {
    return Optional.ofNullable(userMetadata)
        .map(metadata -> metadata.get(USER_TYPE_KEY))
        .map(Object::toString)
        .map(String::toUpperCase)
        .flatMap(this::parseUserType)
        .orElseGet(
            () -> {
              log.debug("No user_type in metadata, defaulting to RESEARCHER");
              return UserType.RESEARCHER;
            });
  }

  private Optional<UserType> parseUserType(String typeStr) {
    try {
      return Optional.of(UserType.valueOf(typeStr));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid user_type '{}' in metadata, defaulting to RESEARCHER", forJava(typeStr));
      return Optional.empty();
    }
  }

  private JUser createUserByType(SupabaseAuthWebhook webhook, UserType userType) {
    var userId = randomUUID().toString();
    var createdAt = convertToLocalDateTime(webhook.user().createdAt());
    var updatedAt = convertToLocalDateTime(webhook.user().updatedAt());

    var user = buildUserEntity(webhook.user().id(), userId, userType, createdAt, updatedAt);
    updateUserFields(user, webhook);

    return user;
  }

  private JUser buildUserEntity(
      String supabaseUserId,
      String userId,
      UserType userType,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    return switch (userType) {
      case RESEARCHER -> buildResearcher(supabaseUserId, userId, createdAt, updatedAt);
      case SELLER -> buildSeller(supabaseUserId, userId, createdAt, updatedAt);
      case MANAGER -> buildManager(supabaseUserId, userId, createdAt, updatedAt);
    };
  }

  private JResearcher buildResearcher(
      String supabaseUserId, String userId, LocalDateTime createdAt, LocalDateTime updatedAt) {
    return JResearcher.builder()
        .id(userId)
        .supabaseUserId(supabaseUserId)
        .phoneNumber("")
        .profileImgUrl("")
        .location(vom.map(Location.getDefault()))
        .userType(UserType.RESEARCHER)
        .status(UserStatus.ENABLED)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }

  private JSeller buildSeller(
      String supabaseUserId, String userId, LocalDateTime createdAt, LocalDateTime updatedAt) {
    return JSeller.builder()
        .id(userId)
        .supabaseUserId(supabaseUserId)
        .phoneNumber("")
        .profileImgUrl("")
        .userType(UserType.SELLER)
        .status(UserStatus.ENABLED)
        .location(vom.map(Location.getDefault()))
        .latLon(vom.map(LatLon.getDefault()))
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }

  private JManager buildManager(
      String supabaseUserId, String userId, LocalDateTime createdAt, LocalDateTime updatedAt) {
    return JManager.builder()
        .id(userId)
        .supabaseUserId(supabaseUserId)
        .phoneNumber("")
        .profileImgUrl("")
        .userType(UserType.MANAGER)
        .status(UserStatus.ENABLED)
        .managerRole(ManagerRole.ADMIN)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }

  private void updateUserFields(JUser user, SupabaseAuthWebhook webhook) {
    var userData = webhook.user();
    Map<String, Object> metadata = userData.userMetadata();

    updateName(user, metadata, userData.email());
    updatePhoneNumber(user, metadata, userData.phone());
    updateProfileImage(user, metadata);
    updateSellerFields(user, metadata);
    updateManagerFields(user, metadata);
  }

  private void updateName(JUser user, Map<String, Object> metadata, String email) {
    extractMetadataValue(metadata, FULL_NAME_KEY)
        .or(() -> extractMetadataValue(metadata, NAME_KEY))
        .or(() -> extractEmailUsername(email))
        .ifPresent(user::setName);
  }

  private void updatePhoneNumber(JUser user, Map<String, Object> metadata, String phone) {
    Optional.ofNullable(phone)
        .filter(p -> !p.isBlank())
        .or(() -> extractMetadataValue(metadata, PHONE_KEY))
        .ifPresent(user::setPhoneNumber);
  }

  private void updateProfileImage(JUser user, Map<String, Object> metadata) {
    extractMetadataValue(metadata, AVATAR_URL_KEY)
        .or(() -> extractMetadataValue(metadata, PROFILE_IMG_URL_KEY))
        .ifPresent(user::setProfileImgUrl);
  }

  private void updateSellerFields(JUser user, Map<String, Object> metadata) {
    if (user instanceof JSeller seller) {
      extractMetadataValue(metadata, GARAGE_NAME_KEY).ifPresent(seller::setGarageName);
    }
  }

  private void updateManagerFields(JUser user, Map<String, Object> metadata) {
    if (user instanceof JManager manager) {
      extractMetadataValue(metadata, MANAGER_ROLE_KEY)
          .flatMap(this::parseManagerRole)
          .ifPresent(manager::setManagerRole);
    }
  }

  private Optional<ManagerRole> parseManagerRole(String roleStr) {
    try {
      return Optional.of(ManagerRole.valueOf(roleStr.toUpperCase()));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid manager_role '{}' in metadata, keeping default", roleStr);
      return Optional.empty();
    }
  }

  private Optional<String> extractMetadataValue(Map<String, Object> metadata, String key) {
    return Optional.ofNullable(metadata).map(m -> m.get(key)).map(Object::toString);
  }

  private Optional<String> extractEmailUsername(String email) {
    return Optional.ofNullable(email)
        .map(e -> e.split("@"))
        .filter(parts -> parts.length > 0)
        .map(parts -> parts[0]);
  }

  private LocalDateTime convertToLocalDateTime(Instant instant) {
    return ofInstant(instant, ZoneOffset.UTC);
  }
}
