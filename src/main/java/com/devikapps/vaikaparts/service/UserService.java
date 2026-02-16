package com.devikapps.vaikaparts.service;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.config.sec.SecContextUtil;
import com.devikapps.vaikaparts.exception.UserNotFoundException;
import com.devikapps.vaikaparts.mapper.user.UserMapper;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.user.User;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import com.devikapps.vaikaparts.service.util.Paginator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final ProfilePhotoService profilePhotoService;
  private final UserMapper um;
  private final Paginator paginator;

  @Transactional(readOnly = true)
  public JUser getCurrentUser() {
    String supabaseUserId = SecContextUtil.getCurrentUserId();
    log.debug("Fetching current user with Supabase ID: {}", forJava(supabaseUserId));

    return userRepository
        .findBySupabaseUserId(supabaseUserId)
        .orElseThrow(
            () -> {
              log.error("User not found for Supabase ID: {}", forJava(supabaseUserId));
              return new UserNotFoundException(
                  format("User not found for Supabase ID: %s", supabaseUserId));
            });
  }

  @Transactional(readOnly = true)
  public User getCurrentUserResponse() {
    var user = getCurrentUser();
    log.info("Fetch user with id : {}", forJava(user.getId()));

    return switch (user.getUserType()) {
      case RESEARCHER -> um.toResearcher((JResearcher) user);
      case SELLER -> um.toSeller((JSeller) user);
      case MANAGER -> um.toManager((JManager) user);
    };
  }

  @Transactional
  public String uploadProfilePhoto(MultipartFile photo) {
    var user = getCurrentUser();
    log.info("Upload user profile image for user with id : {}", forJava(user.getId()));
    return profilePhotoService.uploadPhoto(user, photo);
  }

  @Transactional
  public void deleteProfilePhoto() {
    var user = getCurrentUser();
    log.info("Deleting user profile photo for user with id : {}", forJava(user.getId()));
    profilePhotoService.deletePhoto(user);
  }

  @Transactional(readOnly = true)
  public JResearcher getCurrentResearcher() {
    var user = getCurrentUser();
    validateUserType(user, UserType.RESEARCHER);

    log.debug("Current user is a Researcher: {}", forJava(user.getId()));
    return (JResearcher) user;
  }

  @Transactional(readOnly = true)
  public JSeller getCurrentSeller() {
    var user = getCurrentUser();
    validateUserType(user, UserType.SELLER);

    log.debug("Current user is a Seller: {}", forJava(user.getId()));
    return (JSeller) user;
  }

  @Transactional(readOnly = true)
  public JManager getCurrentManager() {
    var user = getCurrentUser();
    validateUserType(user, UserType.MANAGER);

    log.debug("Current user is a Manager: {}", forJava(user.getId()));
    return (JManager) user;
  }

  @Transactional(readOnly = true)
  public JUser getUserBySupabaseId(String supabaseUserId) {
    log.debug("Fetching user with Supabase ID: {}", forJava(supabaseUserId));

    return userRepository
        .findBySupabaseUserId(supabaseUserId)
        .orElseThrow(
            () -> {
              log.error("User not found for Supabase ID : {}", forJava(supabaseUserId));
              return new UserNotFoundException(
                  format("User not found for Supabase ID: %s", supabaseUserId));
            });
  }

  public Page<JUser> getJUsers(Integer page, Integer size, UserType userType) {
    log.info("Retrieving lists of user with page {} and size {} and type {}", page, size, userType);

    var pagination = paginator.apply(page, size);
    Pageable pageable =
        PageRequest.of(
            pagination.get("page"), pagination.get("size"), Sort.by("createdAt").descending());

    return userRepository.findAllByUserType(userType, pageable);
  }

  private void validateUserType(JUser user, UserType expectedType) {
    if (user.getUserType() != expectedType) {
      log.error(
          "User type mismatch. Expected: {}, Actual: {} for user: {}",
          expectedType,
          user.getUserType(),
          forJava(user.getId()));
      throw new IllegalStateException(
          format("User is not a %s. Actual type: %s", expectedType, user.getUserType()));
    }
  }
}
