package com.devikapps.vaikaparts.service.notification;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.IN_APP;
import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.exception.NotificationDeliveryException;
import com.devikapps.vaikaparts.exception.UserNotFoundException;
import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JNotification;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannel {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final NotificationRequestedRepository notificationRequestedRepository;
  private final NotificationWebSocketService webSocketService;
  private final DemandRepository demandRepository;

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
    var jNotificationRequested =
        notificationRequestedRepository.getReferenceById(notification.getNotificationRequestedId());
    var jSeller =
        userRepository
            .findJUserById(notification.getSeller().getId())
            .map(u -> (JSeller) u)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        format(
                            "No seller with id=%s not found.", notification.getSeller().getId())));
    var jDemand = demandRepository.getReferenceById(notification.getDemand().getId());

    var jNotification =
        JNotification.builder()
            .id(notification.getId())
            .notificationRequested(jNotificationRequested)
            .seller(jSeller)
            .demand(jDemand)
            .message(notification.getMessage())
            .notificationType(notification.getNotificationType())
            .read(notification.isRead())
            .clickAction(notification.getClickAction())
            .createdAt(notification.getCreatedAt())
            .readAt(notification.getReadAt())
            .build();

    notificationRepository.save(jNotification);
    log.debug("Saved notification to database with id={}", forJava(notification.getId()));
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
