package com.devikapps.vaikaparts.service;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.config.sec.SecContextUtil;
import com.devikapps.vaikaparts.exception.UserNotFoundException;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

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
  public JResearcher getCurrentResearcher() {
    JUser user = getCurrentUser();
    validateUserType(user, UserType.RESEARCHER);

    log.debug("Current user is a Researcher: {}", forJava(user.getId()));
    return (JResearcher) user;
  }

  @Transactional(readOnly = true)
  public JSeller getCurrentSeller() {
    JUser user = getCurrentUser();
    validateUserType(user, UserType.SELLER);

    log.debug("Current user is a Seller: {}", forJava(user.getId()));
    return (JSeller) user;
  }

  @Transactional(readOnly = true)
  public JManager getCurrentManager() {
    JUser user = getCurrentUser();
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
