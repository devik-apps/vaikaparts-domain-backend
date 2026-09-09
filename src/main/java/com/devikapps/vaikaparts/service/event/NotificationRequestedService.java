package com.devikapps.vaikaparts.service.event;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.event.model.NotificationRequested;
import com.devikapps.vaikaparts.exception.NotificationRequestedException;
import com.devikapps.vaikaparts.exception.UserNotFoundException;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandPublishedRequestedRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotificationRequested;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRequestedService implements Consumer<NotificationRequested> {

  private final NotificationRequestedRepository notificationRequestedRepository;
  private final DemandPublishedRequestedRepository demandPublishedRequestedRepository;
  private final DemandRepository demandRepository;
  private final SellerMapper sellerMapper;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  @Override
  @Transactional
  public void accept(NotificationRequested event) {
    log.info(
        "[NOTIF-PIPELINE][CHILD] Processing NotificationRequested event: {}, seller: {}, demand:"
            + " {}, attempt: {}",
        forJava(event.getId()),
        forJava(event.getSellerId()),
        forJava(event.getDemandId()),
        event.getAttemptNb());

    var eventLog = createOrUpdateEventLog(event);
    log.info(
        "[NOTIF-PIPELINE][CHILD] Event log resolved: eventId={}, parentId={}, recipientType=SELLER",
        forJava(event.getId()),
        forJava(event.getDemandPublishedRequestedId()));

    try {
      updateEventLogStatus(eventLog, ProcessStatus.PROCESSING);

      var demand = eventLog.getDemand();
      var notificationRequest = buildNotificationRequest(demand, event.getSellerId());
      notificationRequest.setNotificationRequestedId(event.getId());

      notificationService.createAndSendNotification(notificationRequest);

      updateEventLogStatus(eventLog, ProcessStatus.SUCCESS);
      eventLog.setCompletedAt(LocalDateTime.now());
      notificationRequestedRepository.save(eventLog);

      log.info(
          "[NOTIF-PIPELINE][CHILD] Successfully processed NotificationRequested: {}, seller: {}",
          forJava(event.getId()),
          forJava(event.getSellerId()));

    } catch (Exception e) {
      handleEventProcessingError(eventLog, event, e);
    }
  }

  private JDemandPublishedNotificationRequested createOrUpdateEventLog(
      NotificationRequested event) {
    return notificationRequestedRepository
        .findById(event.getId())
        .orElseGet(() -> createNewEventLog(event));
  }

  private JDemandPublishedNotificationRequested createNewEventLog(NotificationRequested event) {
    var parent = fetchParentEventLog(event.getDemandPublishedRequestedId());
    var seller = fetchSeller(event.getSellerId());
    var demand = fetchDemand(event.getDemandId());
    var now = LocalDateTime.now();

    return JDemandPublishedNotificationRequested.builder()
        .id(event.getId())
        .demandPublishedRequested(parent)
        .seller(sellerMapper.toPersistence(seller))
        .demand(demand)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .status(ProcessStatus.PENDING)
        .attemptNb(event.getAttemptNb())
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private void updateEventLogStatus(
      JDemandPublishedNotificationRequested eventLog, ProcessStatus status) {
    eventLog.setStatus(status);
    eventLog.setUpdatedAt(LocalDateTime.now());
    notificationRequestedRepository.save(eventLog);
    log.info(
        "[NOTIF-PIPELINE][EVENT_LOG] Save returned: eventId={}, status={} (commit may be pending)",
        forJava(eventLog.getId()),
        status);
  }

  private JDemandPublishedRequested fetchParentEventLog(String parentId) {
    return demandPublishedRequestedRepository
        .findById(parentId)
        .orElseThrow(
            () -> {
              log.error(
                  "[NOTIF-PIPELINE][CHILD] Parent event log not found: {}", forJava(parentId));
              return new IllegalStateException("Parent event log not found: " + parentId);
            });
  }

  private Seller fetchSeller(String sellerId) {
    var jSeller =
        (JSeller)
            userRepository
                .findJUserById(sellerId)
                .orElseThrow(
                    () ->
                        new UserNotFoundException(
                            format("No seller with id=%s not found", forJava(sellerId))));
    return sellerMapper.toSeller((jSeller));
  }

  private JDemand fetchDemand(String demandId) {
    return demandRepository
        .findById(demandId)
        .orElseThrow(
            () -> {
              log.error("[NOTIF-PIPELINE][CHILD] Demand not found: {}", forJava(demandId));
              return new IllegalStateException("Demand not found: " + demandId);
            });
  }

  private NotificationRequest buildNotificationRequest(JDemand demand, String sellerId) {
    var part = demand.getPart();
    var message =
        format(
            "New demand: %s %s %s (%d)",
            part.getCarBrand(), part.getCarModel(), part.getPartName(), part.getCarYear());

    var clickAction = format("{\"action\":\"VIEW_DEMAND\",\"demandId\":\"%s\"}", demand.getId());

    return NotificationRequest.builder()
        .recipientUserId(sellerId)
        .resourceId(demand.getId())
        .message(message)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .clickAction(clickAction)
        .build();
  }

  private void handleEventProcessingError(
      JDemandPublishedNotificationRequested eventLog, NotificationRequested event, Exception e) {

    log.error(
        "[NOTIF-PIPELINE][CHILD] Failed to process NotificationRequested: {}, seller: {}, attempt:"
            + " {}",
        forJava(event.getId()),
        forJava(event.getSellerId()),
        event.getAttemptNb(),
        e);

    eventLog.setStatus(ProcessStatus.FAILED);
    eventLog.setAttemptNb(event.getAttemptNb());
    eventLog.setErrorMessage(e.getMessage());
    eventLog.setUpdatedAt(LocalDateTime.now());
    eventLog.setCompletedAt(LocalDateTime.now());
    notificationRequestedRepository.save(eventLog);

    throw new NotificationRequestedException("NotificationRequested processing failed", e);
  }
}
