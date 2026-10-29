package com.devikapps.vaikaparts.service.notification.push;

import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.service.notification.NotificationMessageResolver;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record PushDispatchRequest(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("userId") String userId,
    @JsonProperty("title") String title,
    @JsonProperty("body") String body,
    @JsonProperty("url") String url) {

  public static PushDispatchRequest from(
      Notification notification, NotificationMessageResolver resolver) {
    var eventId = notification.getId();
    if (eventId == null || eventId.isBlank() || eventId.length() > 128) {
      throw new IllegalArgumentException("Web push requires a bounded notification ID");
    }

    String userId;
    try {
      var suppliedUserId = notification.getRecipient().getSupabaseUserId();
      var parsedUserId = UUID.fromString(suppliedUserId);
      if (!parsedUserId.toString().equalsIgnoreCase(suppliedUserId)) {
        throw new IllegalArgumentException();
      }
      userId = parsedUserId.toString();
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("Web push requires a Supabase user UUID");
    }

    var body =
        resolver.resolve(
            notification.getNotificationType(),
            notification.getRecipient().getPreferredLanguage(),
            notification.getResource(),
            notification.getMessage());
    body = boundBody(body == null ? "" : body);

    return new PushDispatchRequest(
        eventId,
        userId,
        "VaikaParts",
        body,
        WebPushPayload.safeClickAction(notification.getClickAction()));
  }

  private static String boundBody(String body) {
    if (body.length() <= 300) return body;
    int end = 300;
    if (Character.isHighSurrogate(body.charAt(end - 1))
        && Character.isLowSurrogate(body.charAt(end))) end--;
    return body.substring(0, end);
  }
}
