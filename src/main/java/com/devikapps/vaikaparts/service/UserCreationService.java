package com.devikapps.vaikaparts.service;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.user.ProfileRecord;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.LatLon;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.*;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import com.devikapps.vaikaparts.service.util.NotificationPreferencesUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.devikapps.vaikaparts.model.classifier.UserType.RESEARCHER;
import static com.devikapps.vaikaparts.model.classifier.UserType.SELLER;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCreationService {
    private final UserRepository userRepository;
    private final ValueObjectMapper vom;
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
    private static final String CATEGORY_LIST_KEY = "category_list";
    private static final String HANDLE_ALL_CATEGORY_KEY = "handle_all_category";
    private static final String IS_DELIVERYING_KEY = "is_deliverying";

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createUserIfAbsent(ProfileRecord profile, UserType userType) {
        var profileId = profile.id();
        userRepository.lockByKey(userLockKey(profileId));

        var existingUser = userRepository.findBySupabaseUserId(profileId);
        if (existingUser.isPresent()) {
            updateUserFields(existingUser.get(), profile);
            existingUser.get().setUpdatedAt(profile.updatedAt());
            userRepository.save(existingUser.get());
            log.info("User {} already exists, updated from webhook", forJava(profileId));
            return;
        }

        try {
            var newUser = createUserByType(profile, userType);
            userRepository.saveAndFlush(newUser);
            log.info("Successfully created {} for profile {}",
                    userType, forJava(profileId));
        } catch (DataIntegrityViolationException e) {
            log.warn("User {} already exists, updating existing row from webhook", forJava(profileId));
            userRepository.findBySupabaseUserId(profileId)
                    .ifPresent(existing -> {
                        updateUserFields(existing, profile);
                        existing.setUpdatedAt(profile.updatedAt());
                        userRepository.save(existing);
                    });
        }
    }

    private String userLockKey(String profileId) {
        return "user:supabase:" + profileId;
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
            String name,
            String email,
            String phoneNumber,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {

        var profileId = profile.id();
        var metadata = profile.userMetadata();
        var appMetadata = profile.appMetadata();
        return switch (userType) {
            case RESEARCHER -> {
                var location = extractLocation(metadata).orElse(null);
                yield buildResearcher(
                        profileId, name, email, phoneNumber, userId, location, createdAt, updatedAt);
            }
            case SELLER -> {
                var garageName = extractMetadataValue(metadata, GARAGE_NAME_KEY).orElse(null);
                var location = extractLocation(metadata).orElse(null);
                var latLon = extractLatLon(metadata).orElse(null);
                var categoryList = extractCategoryList(metadata).orElse(new ArrayList<>());
                var handleAllCategory = extractHandleAllCategory(metadata).orElse(true);
                var isDeliverying = extractIsDeliverying(metadata).orElse(false);
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
                        updatedAt,
                        categoryList,
                        handleAllCategory,
                        isDeliverying);
            }
            case MANAGER -> {
                var managerRole =
                        extractMetadataValue(appMetadata, MANAGER_ROLE_KEY)
                                .flatMap(this::parseManagerRole)
                                .orElse(ManagerRole.ADMIN);
                yield buildManager(
                        profileId, userId, name, email, phoneNumber, managerRole, createdAt, updatedAt);
            }
        };
    }

    private Optional<Boolean> extractHandleAllCategory(Map<String, Object> metadata) {
        return Optional.ofNullable(metadata).map(m -> m.get(HANDLE_ALL_CATEGORY_KEY)).filter(Boolean.class::isInstance).map(Boolean.class::cast);
    }

    private Optional<Boolean> extractIsDeliverying(Map<String, Object> metadata) {
        return Optional.ofNullable(metadata)
                .map(m -> m.get(IS_DELIVERYING_KEY))
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast);
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
            OffsetDateTime updatedAt,
            List<PartCategory> categoryList,
            Boolean handleAllCategory,
            Boolean isDeliverying) {
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
                .categoryList(categoryList)
                .handleAllCategory(handleAllCategory)
                .isDeliverying(isDeliverying)
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
    public void updateUserFields(JUser user, ProfileRecord profile) {
        Map<String, Object> metadata = profile.userMetadata();
        Map<String,Object> appMetadata = profile.appMetadata();
        updateName(user, profile.name());
        updatePhoneNumber(user, profile.phoneNumber());
        updateProfileImage(user, profile.profileImgUrl());
        updateLocationFields(user, metadata);
        updateSellerFields(user, metadata);
        NotificationPreferencesUpdater.apply(user, metadata);
        updateManagerFields(user, appMetadata);
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

    public void updateResearcherLocation(JUser user, Map<String, Object> metadata) {
        if (user instanceof JResearcher researcher)
            extractLocation(metadata).ifPresent(location -> researcher.setLocation(vom.map(location)));
    }

    public void updateSellerLocationAndLatLon(JUser user, Map<String, Object> metadata) {
        if (user instanceof JSeller seller) {
            extractLocation(metadata).ifPresent(location -> seller.setLocation(vom.map(location)));
            extractLatLon(metadata).ifPresent(latLon -> seller.setLatLon(vom.map(latLon)));
        }
    }

    public void updateSellerFields(JUser user, Map<String, Object> metadata) {
        if (user instanceof JSeller seller) {
            extractMetadataValue(metadata, GARAGE_NAME_KEY)
                    .filter(name -> !name.isBlank())
                    .ifPresent(seller::setGarageName);
            extractCategoryList(metadata).ifPresent(seller::setCategoryList);
            extractHandleAllCategory(metadata).ifPresent(seller::setHandleAllCategory);
            extractIsDeliverying(metadata).ifPresent(seller::setIsDeliverying);
        }
    }

    private void updateManagerFields(JUser user, Map<String, Object> metadata) {
        if (user instanceof JManager manager) {
            extractMetadataValue(metadata, MANAGER_ROLE_KEY)
                    .flatMap(this::parseManagerRole)
                    .ifPresent(manager::setManagerRole);
        }
    }
    private Optional<List<PartCategory>> extractCategoryList(Map<String, Object> metadata) {
        return Optional.ofNullable(metadata)
                .map(m -> m.get(CATEGORY_LIST_KEY))
                .filter(List.class::isInstance)
                .map(obj -> (List<?>) obj)
                .map(list -> list.stream()
                        .map(this::parsePartCategory)
                        .flatMap(Optional::stream)
                        .distinct()
                        .collect(Collectors.toList()));
    }

    private Optional<PartCategory> parsePartCategory(Object value) {
        if (value instanceof PartCategory category) return Optional.of(category);
        if (value instanceof String categoryName) {
            try {
                return Optional.of(PartCategory.valueOf(categoryName));
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring invalid seller category '{}' in user metadata", forJava(categoryName));
            }
        } else {
            log.warn("Ignoring seller category with invalid type in user metadata");
        }
        return Optional.empty();
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

    public Optional<String> extractMetadataValue(Map<String, Object> metadata, String key) {
        return Optional.ofNullable(metadata).map(m -> m.get(key)).map(Object::toString);
    }

    private Optional<String> extractEmailUsername(String email) {
        return Optional.ofNullable(email)
                .map(e -> e.split("@"))
                .filter(parts -> parts.length > 0)
                .map(parts -> parts[0]);
    }
}
