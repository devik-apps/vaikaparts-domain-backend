package com.devikapps.vaikaparts.service.notification;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.IN_APP;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.exception.NotificationDeliveryException;
import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannel {

  private final DemandPublishedNotificationRepository demandPublishedNotificationRepository;
  private final UserRepository userRepository;
  private final NotificationRequestedRepository notificationRequestedRepository;
  private final NotificationWebSocketService webSocketService;
  private final DemandRepository demandRepository;
  private final OfferRepository offerRepository;

  @Override
  public void send(Notification notification) {
    log.info(
        "Sending in-app notification type={} to user={} ({})",
        notification.getNotificationType(),
        forJava(notification.getRecipient().getId()),
        notification.getRecipient().getUserType());

    try {
      saveNotificationToDatabase(notification);
      sendViaWebSocket(notification);

      log.info("Successfully sent in-app notification: {}", forJava(notification.getId()));
    } catch (Exception e) {
      log.error("Failed to send in-app notification: {}", forJava(notification.getId()), e);
      throw new NotificationDeliveryException(
          "Failed to send in-app notification to user: " + notification.getRecipient().getId(), e);
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
        notification.getNotificationRequestedId() == null
            ? null
            : notificationRequestedRepository.getReferenceById(
                notification.getNotificationRequestedId());
    var jRecipient = userRepository.getReferenceById(notification.getRecipient().getId());
    var jDemand =
        notification.getResource() instanceof Demand demand
            ? demandRepository.getReferenceById(demand.getId())
            : null;
    var jOffer =
        notification.getResource() instanceof Offer offer
            ? offerRepository.getReferenceById(offer.getId())
            : null;

    var jNotification =
        JDemandPublishedNotification.builder()
            .id(notification.getId())
            .notificationRequested(jNotificationRequested)
            .recipient(jRecipient)
            .demand(jDemand)
            .offer(jOffer)
            .message(notification.getMessage())
            .notificationType(notification.getNotificationType())
            .read(notification.isRead())
            .clickAction(notification.getClickAction())
            .createdAt(notification.getCreatedAt())
            .readAt(notification.getReadAt())
            .build();

    demandPublishedNotificationRepository.save(jNotification);
    log.debug("Saved notification to database with id={}", forJava(notification.getId()));
  }

  private void sendViaWebSocket(Notification notification) {
    try {
      webSocketService.sendNotificationToUser(notification.getRecipient().getId(), notification);
      log.debug(
          "Sent WebSocket notification to user={} ({})",
          forJava(notification.getRecipient().getId()),
          notification.getRecipient().getUserType());
    } catch (Exception e) {
      log.warn(
          "WebSocket delivery failed for user={} ({}) (notification saved in DB)",
          forJava(notification.getRecipient().getId()),
          notification.getRecipient().getUserType(),
          e);
    }
  }
}
