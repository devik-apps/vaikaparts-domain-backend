package com.devikapps.vaikaparts.service.notification;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.config.sec.SecContextUtil;
import com.devikapps.vaikaparts.model.notification.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {

  public static final String TOPIC_NOTIFICATIONS_ENDPOINT = "/topic/notifications/";
  private final SimpMessagingTemplate messagingTemplate;

  public void sendNotificationToUser(String userId, Notification notification) {
    log.info(
        "WebSocket: Sending notification type={} to user={} ({})",
        notification.getNotificationType(),
        forJava(userId),
        notification.getRecipient().getUserType());

    String supabaseUserId = SecContextUtil.getCurrentUserId();
    log.debug("Authenticated user sending notification: {}", forJava(supabaseUserId));

    String destination = TOPIC_NOTIFICATIONS_ENDPOINT + userId;
    messagingTemplate.convertAndSend(destination, notification);

    log.debug(
        "WebSocket notification sent to destination={} for notification id={}",
        destination,
        forJava(notification.getId()));
  }
}
