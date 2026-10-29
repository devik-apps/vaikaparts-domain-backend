package com.devikapps.vaikaparts.service.notification.push;

import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.service.notification.NotificationMessageResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Deliberately contains no recipient, resource, or profile objects. */
public record WebPushPayload(String id, String type, String message, String clickAction) {
  public static byte[] from(
      Notification notification, NotificationMessageResolver resolver, ObjectMapper mapper) {
    if (notification.getId() == null || notification.getId().length() > 128) {
      throw new IllegalArgumentException("Web push requires a bounded notification ID");
    }
    String message =
        resolver.resolve(
            notification.getNotificationType(),
            notification.getRecipient().getPreferredLanguage(),
            notification.getResource(),
            notification.getMessage());
    message = message == null ? "" : message.substring(0, Math.min(256, message.length()));
    var payload =
        new WebPushPayload(
            notification.getId(),
            notification.getNotificationType().name(),
            message,
            safeClickAction(notification.getClickAction()));
    try {
      var bytes = mapper.writeValueAsBytes(payload);
      if (bytes.length > 3072) {
        bytes =
            mapper.writeValueAsBytes(
                new WebPushPayload(
                    payload.id(), payload.type(), payload.message(), "/notifications"));
      }
      return bytes;
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Unable to encode web push payload");
    }
  }

  static String safeClickAction(String action) {
    try {
      if (action == null || action.length() > 256) return "/notifications";
      // Decode nested encodings as well: browsers and application routers may normalize them.
      String decoded = action;
      for (int i = 0; i <= action.length(); i++) {
        if (!decoded.startsWith("/")
            || decoded.startsWith("//")
            || decoded.contains("\\")
            || decoded.chars().anyMatch(c -> c <= 32 || c == 127)) return "/notifications";
        String next = URLDecoder.decode(decoded.replace("+", "%2B"), StandardCharsets.UTF_8);
        if (next.equals(decoded)) break;
        decoded = next;
      }
      var uri = new URI(action);
      if (uri.isAbsolute() || uri.getRawAuthority() != null) return "/notifications";
      return action;
    } catch (IllegalArgumentException | java.net.URISyntaxException exception) {
      return "/notifications";
    }
  }
}
