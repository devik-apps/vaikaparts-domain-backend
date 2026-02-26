package com.devikapps.vaikaparts.service.notification;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.IN_APP;
import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.exception.NotificationDeliveryException;
import com.devikapps.vaikaparts.exception.UserNotFoundException;
import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.notification.DemandPublishedNotification;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.NotificationChannel;
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

  @Override
  public void send(DemandPublishedNotification demandPublishedNotification) {
    log.info(
        "Sending in-app notification to seller: {}",
        forJava(demandPublishedNotification.getSeller().getId()));

    try {
      saveNotificationToDatabase(demandPublishedNotification);
      sendViaWebSocket(demandPublishedNotification);

      log.info(
          "Successfully sent in-app notification: {}",
          forJava(demandPublishedNotification.getId()));
    } catch (Exception e) {
      log.error(
          "Failed to send in-app notification: {}",
          forJava(demandPublishedNotification.getId()),
          e);
      throw new NotificationDeliveryException(
          "Failed to send in-app notification to seller: "
              + demandPublishedNotification.getSeller().getId(),
          e);
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

  private void saveNotificationToDatabase(DemandPublishedNotification demandPublishedNotification) {
    var jNotificationRequested =
        notificationRequestedRepository.getReferenceById(
            demandPublishedNotification.getNotificationRequestedId());
    var jSeller =
        userRepository
            .findJUserById(demandPublishedNotification.getSeller().getId())
            .map(u -> (JSeller) u)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        format(
                            "No seller with id=%s not found.",
                            demandPublishedNotification.getSeller().getId())));
    var jDemand =
        demandRepository.getReferenceById(demandPublishedNotification.getDemand().getId());

    var jNotification =
        JDemandPublishedNotification.builder()
            .id(demandPublishedNotification.getId())
            .notificationRequested(jNotificationRequested)
            .seller(jSeller)
            .demand(jDemand)
            .message(demandPublishedNotification.getMessage())
            .notificationType(demandPublishedNotification.getNotificationType())
            .read(demandPublishedNotification.isRead())
            .clickAction(demandPublishedNotification.getClickAction())
            .createdAt(demandPublishedNotification.getCreatedAt())
            .readAt(demandPublishedNotification.getReadAt())
            .build();

    demandPublishedNotificationRepository.save(jNotification);
    log.debug(
        "Saved notification to database with id={}", forJava(demandPublishedNotification.getId()));
  }

  private void sendViaWebSocket(DemandPublishedNotification demandPublishedNotification) {
    try {
      webSocketService.sendNotificationToSeller(
          demandPublishedNotification.getSeller().getId(), demandPublishedNotification);
      log.debug(
          "Sent WebSocket notification to seller: {}",
          forJava(demandPublishedNotification.getSeller().getId()));
    } catch (Exception e) {
      log.warn(
          "WebSocket delivery failed for seller: {} (notification saved in DB)",
          forJava(demandPublishedNotification.getSeller().getId()),
          e);
    }
  }
}
