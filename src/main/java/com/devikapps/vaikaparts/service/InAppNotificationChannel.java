package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.IN_APP;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.exception.NotificationDeliveryException;
import com.devikapps.vaikaparts.mapper.NotificationMapper;
import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannel {

  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;
  private final NotificationWebSocketService webSocketService;

  @Override
  public void send(Notification notification) {
    log.info(
        "Sending in-app notification to seller: {}", forJava(notification.getSeller().getId()));

    try {
      saveNotificationToDatabase(notification);
      sendViaWebSocket(notification);

      log.info("Successfully sent in-app notification: {}", forJava(notification.getId()));
    } catch (Exception e) {
      log.error("Failed to send in-app notification: {}", forJava(notification.getId()), e);
      throw new NotificationDeliveryException(
          "Failed to send in-app notification to seller: " + notification.getSeller().getId(), e);
    }
  }

  @Override
  public NotificationChannelType getChannelType() {
    return IN_APP;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  private void saveNotificationToDatabase(Notification notification) {
    var jNotification = notificationMapper.toPersistence(notification);
    notificationRepository.save(jNotification);
    log.debug(
        "Saved notification to database with notification id={}", forJava(notification.getId()));
  }

  private void sendViaWebSocket(Notification notification) {
    try {
      webSocketService.sendNotificationToSeller(notification.getSeller().getId(), notification);
      log.debug(
          "Sent WebSocket notification to seller: {}", forJava(notification.getSeller().getId()));
    } catch (Exception e) {
      log.warn(
          "WebSocket delivery failed for seller: {} (notification saved in DB)",
          forJava(notification.getSeller().getId()),
          e);
    }
  }
}
