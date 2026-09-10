package com.devikapps.vaikaparts.service;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.user.SupabaseWebhook;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import java.time.OffsetDateTime;
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
  private final UserCreationService userCreationService;

  private final UserRepository userRepository;

  @Transactional
  public void handleUserCreated(SupabaseWebhook webhook) {
    var profile = webhook.record();
    var profileId = profile.id();
    log.info("webhook body {}", webhook);
    log.info("Processing INSERT event for profile ID: {}", forJava(profileId));

    log.info("metadata {}", profile.appMetadata());
    extractUserType(profile.appMetadata())
        .ifPresentOrElse(
            userType -> userCreationService.createUserIfAbsent(profile, userType),
            () ->
                log.info(
                    "Skipping INSERT for profile {} because user_type is not available yet",
                    forJava(profileId)));
  }

  @Transactional
  public void handleUserUpdated(SupabaseWebhook webhook) {
    var profile = webhook.record();
    var profileId = profile.id();

    log.info("Processing UPDATE event for profile ID: {}", forJava(profileId));

    userRepository
        .findBySupabaseUserId(profileId)
        .ifPresentOrElse(
            user -> {
              userCreationService.updateUserFields(user, profile);
              user.setUpdatedAt(profile.updatedAt());
              userRepository.save(user);

              log.info(
                  "Successfully updated user {} for profile {}",
                  forJava(user.getId()),
                  forJava(profileId));
            },
            () ->
                extractUserType(profile.appMetadata())
                    .ifPresentOrElse(
                        userType -> userCreationService.createUserIfAbsent(profile, userType),
                        () ->
                            log.info(
                                "Skipping UPDATE for unknown profile {} because user_type is not"
                                    + " available yet",
                                forJava(profileId))));
  }

  @Transactional
  public void handleUserDeleted(SupabaseWebhook webhook) {
    var profile = webhook.oldRecord();
    var profileId = profile.id();

    log.info("Processing DELETE event for profile ID: {}", forJava(profileId));

    var user = findUserBySupabaseId(profileId);
    user.setStatus(UserStatus.DISABLED);
    user.setUpdatedAt(OffsetDateTime.now());

    userRepository.save(user);
    log.info(
        "Successfully soft-deleted user {} for profile {}",
        forJava(user.getId()),
        forJava(profileId));
  }

  private JUser findUserBySupabaseId(String profileId) {
    return userRepository
        .findBySupabaseUserId(profileId)
        .orElseThrow(
            () -> {
              log.error("User not found for profile ID: {}", forJava(profileId));
              return new IllegalStateException("Cannot process non-existent user: " + profileId);
            });
  }

  private Optional<UserType> extractUserType(Map<String, Object> appMetadata) {
    log.info(
        "ROLE USER IN METADATA {}",
        userCreationService.extractMetadataValue(appMetadata, USER_TYPE_KEY).orElse(null));
    return Optional.ofNullable(appMetadata)
        .map(metadata -> metadata.get(USER_TYPE_KEY))
        .map(Object::toString)
        .map(String::toUpperCase)
        .flatMap(this::parseUserType);
  }

  private Optional<UserType> parseUserType(String typeStr) {
    try {
      return Optional.of(UserType.valueOf(typeStr));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid user_type '{}' in metadata", forJava(typeStr));
      return Optional.empty();
    }
  }
}
