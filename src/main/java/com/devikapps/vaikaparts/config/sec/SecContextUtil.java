package com.devikapps.vaikaparts.config.sec;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public final class SecContextUtil {

  private SecContextUtil() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  public static String getCurrentUserId() {
    var authentication = getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("Attempted to get current user ID but no authentication found");
      throw new AuthenticationCredentialsNotFoundException("No authenticated user found");
    }

    var principal = authentication.getPrincipal();

    if (principal instanceof String userId) return userId;

    log.error("Authentication principal is not a String userId: {}", principal.getClass());
    throw new IllegalStateException(
        "Invalid authentication principal type: " + principal.getClass().getName());
  }

  public static Optional<String> getCurrentUserIdOptional() {
    try {
      return Optional.of(getCurrentUserId());
    } catch (AuthenticationCredentialsNotFoundException | IllegalStateException e) {
      log.debug("Could not retrieve current user ID: {}", e.getMessage());
      return Optional.empty();
    }
  }

  public static boolean isAuthenticated() {
    var authentication = getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof String;
  }

  private static Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }
}
