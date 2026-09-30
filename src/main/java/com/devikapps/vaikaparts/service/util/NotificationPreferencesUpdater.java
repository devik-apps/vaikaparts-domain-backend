package com.devikapps.vaikaparts.service.util;

import com.devikapps.vaikaparts.repository.model.user.JUser;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/** Applies explicit boolean preferences only; missing or invalid values preserve existing choices. */
@Slf4j
public final class NotificationPreferencesUpdater {
  private NotificationPreferencesUpdater() {}

  public static void apply(JUser user, Map<String, Object> metadata) {
    extractBoolean(metadata, "email_notifications_enabled")
        .ifPresent(user::setEmailNotificationsEnabled);
    extractBoolean(metadata, "sms_notifications_enabled")
        .ifPresent(user::setSmsNotificationsEnabled);
  }

  private static Optional<Boolean> extractBoolean(Map<String, Object> metadata, String key) {
    if (metadata == null || !metadata.containsKey(key)) return Optional.empty();
    var value = metadata.get(key);
    if (value instanceof Boolean enabled) return Optional.of(enabled);
    log.warn("Ignoring invalid notification preference '{}': expected a JSON boolean", key);
    return Optional.empty();
  }
}
