package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.UserType.RESEARCHER;
import static com.devikapps.vaikaparts.model.classifier.UserType.SELLER;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.user.ProfileRecord;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.user.SupabaseWebhook;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.LatLon;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.City;
import com.devikapps.vaikaparts.model.classifier.ManagerRole;
import com.devikapps.vaikaparts.model.classifier.Region;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
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
  private static final String NAME_KEY = "name";
  private static final String LOCATION_KEY = "location";
  private static final String LAT_LON_KEY = "lat_lon";
  private static final String GARAGE_NAME_KEY = "garage_name";
  private static final String MANAGER_ROLE_KEY = "manager_role";
  private static final String CITY_KEY = "city";
  private static final String REGION_KEY = "region";
  private static final String ADDRESS_KEY = "address";
  private static final String LAT_KEY = "lat";
  private static final String LON_KEY = "lon";

  private final UserRepository userRepository;
  private final ValueObjectMapper vom;

  @Transactional
  public void handleUserCreated(SupabaseWebhook webhook) {
    var profile = webhook.record();
    var profileId = profile.id();

    log.info("Processing INSERT event for profile ID: {}", forJava(profileId));

    if (userRepository.existsBySupabaseUserId(profileId)) {
      log.warn("User with profile ID {} already exists, skipping creation", forJava(profileId));
      return;
    }

    var userType = extractUserType(profile.userMetadata());
    var newUser = createUserByType(profile, userType);

    userRepository.save(newUser);
    log.info(
        "Successfully created {} with ID {} for profile {}",
        userType,
        forJava(newUser.getId()),
        forJava(profileId));
  }

  @Transactional
  public void handleUserUpdated(SupabaseWebhook webhook) {
    var profile = webhook.record();
    var profileId = profile.id();

    log.info("Processing UPDATE event for profile ID: {}", forJava(profileId));

    var user = findUserBySupabaseId(profileId);
    updateUserFields(user, profile);
    user.setUpdatedAt(profile.updatedAt());

    userRepository.save(user);
    log.info(
        "Successfully updated user {} for profile {}", forJava(user.getId()), forJava(profileId));
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

  private UserType extractUserType(Map<String, Object> userMetadata) {
    return Optional.ofNullable(userMetadata)
        .map(metadata -> metadata.get(USER_TYPE_KEY))
        .map(Object::toString)
        .map(String::toUpperCase)
        .flatMap(this::parseUserType)
        .orElseGet(
            () -> {
              log.debug("No user_type in metadata, defaulting to RESEARCHER");
              return RESEARCHER;
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

  private JUser createUserByType(ProfileRecord profile, UserType userType) {
    var userId = randomUUID().toString();
    var createdAt = profile.createdAt();
    var updatedAt = profile.updatedAt();
    var name = extractName(profile);
    var email = extractEmail(profile);
    var phoneNumber = extractPhoneNumber(profile);

    var user =
        buildUserEntity(profile, userType, userId, name, email, phoneNumber, createdAt, updatedAt);
    updateUserFields(user, profile);

    return user;
  }

  private JUser buildUserEntity(
          ProfileRecord profile,
          UserType userType,
          String userId,
          String email,
          String name,
          String phoneNumber,
          OffsetDateTime createdAt,
          OffsetDateTime updatedAt) {

    var profileId = profile.id();
    var metadata = profile.userMetadata();

    return switch (userType) {
      case RESEARCHER -> {
        var location = extractLocation(metadata).orElse(null);
        yield buildResearcher(
            profileId, email, name, phoneNumber, userId, location, createdAt, updatedAt);
      }
      case SELLER -> {
        var garageName = extractMetadataValue(metadata, GARAGE_NAME_KEY).orElse(null);
        var location = extractLocation(metadata).orElse(null);
        var latLon = extractLatLon(metadata).orElse(null);
        yield buildSeller(
            profileId,
            name,
            email,
            phoneNumber,
            garageName,
            userId,
            location,
            latLon,
            createdAt,
            updatedAt);
      }
      case MANAGER -> {
        var managerRole =
            extractMetadataValue(metadata, MANAGER_ROLE_KEY)
                .flatMap(this::parseManagerRole)
                .orElse(ManagerRole.ADMIN);
        yield buildManager(
            profileId, userId, email, name, phoneNumber, managerRole, createdAt, updatedAt);
      }
    };
  }

  private JResearcher buildResearcher(
      String profileId,
      String name,
      String email,
      String phoneNumber,
      String userId,
      Location location,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {
    var finalLocation = (location == null) ? Location.getDefault() : location;
    return JResearcher.builder()
        .id(userId)
        .supabaseUserId(profileId)
        .name(name)
        .email(email)
        .phoneNumber(phoneNumber)
        .profileImgKey("")
        .location(vom.map(finalLocation))
        .userType(RESEARCHER)
        .status(UserStatus.ENABLED)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }

  private JSeller buildSeller(
      String profileId,
      String name,
      String email,
      String phoneNumber,
      String garageName,
      String userId,
      Location location,
      LatLon latLon,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {
    var finalLocation = (location == null) ? Location.getDefault() : location;
    var finalLatLon = (latLon == null) ? LatLon.getDefault() : latLon;
    return JSeller.builder()
        .id(userId)
        .supabaseUserId(profileId)
        .name(name)
        .email(email)
        .phoneNumber(phoneNumber)
        .profileImgKey("")
        .garageName(garageName)
        .userType(SELLER)
        .status(UserStatus.ENABLED)
        .location(vom.map(finalLocation))
        .latLon(vom.map(finalLatLon))
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }

  private JManager buildManager(
      String profileId,
      String userId,
      String name,
      String email,
      String phoneNumber,
      ManagerRole managerRole,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {
    return JManager.builder()
        .id(userId)
        .supabaseUserId(profileId)
        .name(name)
        .email(email)
        .phoneNumber(phoneNumber)
        .profileImgKey("")
        .userType(UserType.MANAGER)
        .status(UserStatus.ENABLED)
        .managerRole(managerRole)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }

  private void updateUserFields(JUser user, ProfileRecord profile) {
    Map<String, Object> metadata = profile.userMetadata();

    updateName(user, profile.name());
    updatePhoneNumber(user, profile.phoneNumber());
    updateProfileImage(user, profile.profileImgUrl());
    updateLocationFields(user, metadata);
    updateSellerFields(user, metadata);
    updateManagerFields(user, metadata);
  }

  private void updateName(JUser user, String profileName) {
    Optional.ofNullable(profileName).filter(n -> !n.isBlank()).ifPresent(user::setName);
  }

  private void updatePhoneNumber(JUser user, String phoneNumber) {
    Optional.ofNullable(phoneNumber).filter(p -> !p.isBlank()).ifPresent(user::setPhoneNumber);
  }

  private void updateProfileImage(JUser user, String profileImgUrl) {
    Optional.ofNullable(profileImgUrl)
        .filter(url -> !url.isBlank())
        .ifPresent(user::setProfileImgKey);
  }

  private void updateLocationFields(JUser user, Map<String, Object> metadata) {
    if (user.getUserType() == RESEARCHER) updateResearcherLocation(user, metadata);
    else if (user.getUserType() == SELLER) updateSellerLocationAndLatLon(user, metadata);
  }

  private void updateResearcherLocation(JUser user, Map<String, Object> metadata) {
    if (user instanceof JResearcher researcher)
      extractLocation(metadata).ifPresent(location -> researcher.setLocation(vom.map(location)));
  }

  private void updateSellerLocationAndLatLon(JUser user, Map<String, Object> metadata) {
    if (user instanceof JSeller seller) {
      extractLocation(metadata).ifPresent(location -> seller.setLocation(vom.map(location)));
      extractLatLon(metadata).ifPresent(latLon -> seller.setLatLon(vom.map(latLon)));
    }
  }

  private void updateSellerFields(JUser user, Map<String, Object> metadata) {
    if (user instanceof JSeller seller) {
      extractMetadataValue(metadata, GARAGE_NAME_KEY)
          .filter(name -> !name.isBlank())
          .ifPresent(seller::setGarageName);
    }
  }

  private void updateManagerFields(JUser user, Map<String, Object> metadata) {
    if (user instanceof JManager manager) {
      extractMetadataValue(metadata, MANAGER_ROLE_KEY)
          .flatMap(this::parseManagerRole)
          .ifPresent(manager::setManagerRole);
    }
  }

  @SuppressWarnings("unchecked")
  private Optional<Location> extractLocation(Map<String, Object> metadata) {
    return Optional.ofNullable(metadata)
        .map(m -> m.get(LOCATION_KEY))
        .filter(Map.class::isInstance)
        .map(obj -> (Map<String, Object>) obj)
        .flatMap(this::parseLocation);
  }

  private String extractEmail(ProfileRecord profile) {
    return Optional.ofNullable(profile.email()).filter(p -> !p.isBlank()).orElse("");
  }

  private Optional<Location> parseLocation(Map<String, Object> locationMap) {
    var cityStr = (String) locationMap.get(CITY_KEY);
    var regionStr = (String) locationMap.get(REGION_KEY);
    var address = (String) locationMap.get(ADDRESS_KEY);

    if (cityStr == null || regionStr == null || address == null) return Optional.empty();

    return parseCity(cityStr)
        .flatMap(city -> parseRegion(regionStr).map(region -> new Location(city, region, address)));
  }

  private Optional<City> parseCity(String cityStr) {
    try {
      return Optional.of(City.valueOf(cityStr.toUpperCase()));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid city '{}' in metadata", forJava(cityStr));
      return Optional.empty();
    }
  }

  private Optional<Region> parseRegion(String regionStr) {
    try {
      return Optional.of(Region.valueOf(regionStr.toUpperCase()));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid region '{}' in metadata", forJava(regionStr));
      return Optional.empty();
    }
  }

  @SuppressWarnings("unchecked")
  private Optional<LatLon> extractLatLon(Map<String, Object> metadata) {
    return Optional.ofNullable(metadata)
        .map(m -> m.get(LAT_LON_KEY))
        .filter(Map.class::isInstance)
        .map(obj -> (Map<String, Object>) obj)
        .flatMap(this::parseLatLon);
  }

  private Optional<LatLon> parseLatLon(Map<String, Object> latLonMap) {
    var latObj = latLonMap.get(LAT_KEY);
    var lonObj = latLonMap.get(LON_KEY);

    if (latObj == null || lonObj == null) return Optional.empty();

    var lat = ((Number) latObj).doubleValue();
    var lon = ((Number) lonObj).doubleValue();
    return Optional.of(new LatLon(lat, lon));
  }

  private Optional<ManagerRole> parseManagerRole(String roleStr) {
    try {
      return Optional.of(ManagerRole.valueOf(roleStr.toUpperCase()));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid manager_role '{}' in metadata, keeping default", forJava(roleStr));
      return Optional.empty();
    }
  }

  private String extractName(ProfileRecord profile) {
    return Optional.ofNullable(profile.name())
        .filter(n -> !n.isBlank())
        .or(() -> extractMetadataValue(profile.userMetadata(), NAME_KEY))
        .or(() -> extractEmailUsername(profile.email()))
        .orElse("");
  }

  private String extractPhoneNumber(ProfileRecord profile) {
    return Optional.ofNullable(profile.phoneNumber()).filter(p -> !p.isBlank()).orElse("");
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
}
