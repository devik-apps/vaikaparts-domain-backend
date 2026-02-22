package com.devikapps.vaikaparts.service.event;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.event.model.NotificationRequested;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.repository.DemandPublishedRequestedRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
import com.devikapps.vaikaparts.repository.event.JNotificationRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.NotificationService;
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
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  @Override
  @Transactional
  public void accept(NotificationRequested event) {
    log.info(
        "Processing NotificationRequested event: {}, seller: {}, demand: {}, attempt: {}",
        forJava(event.getId()),
        forJava(event.getSellerId()),
        forJava(event.getDemandId()),
        event.getAttemptNb());

    var eventLog = createOrUpdateEventLog(event);

    try {
      updateEventLogStatus(eventLog, ProcessStatus.PROCESSING);

      var demand = fetchDemand(event.getDemandId());
      var notificationRequest = buildNotificationRequest(demand, event.getSellerId());

      notificationService.createAndSendNotification(notificationRequest);

      updateEventLogStatus(eventLog, ProcessStatus.SUCCESS);
      eventLog.setCompletedAt(LocalDateTime.now());
      notificationRequestedRepository.save(eventLog);

      log.info(
          "Successfully processed NotificationRequested: {}, seller: {}",
          forJava(event.getId()),
          forJava(event.getSellerId()));

    } catch (Exception e) {
      handleEventProcessingError(eventLog, event, e);
    }
  }

  private JNotificationRequested createOrUpdateEventLog(NotificationRequested event) {
    return notificationRequestedRepository
        .findById(event.getId())
        .orElseGet(() -> createNewEventLog(event));
  }

  private JNotificationRequested createNewEventLog(NotificationRequested event) {
    var parent = fetchParentEventLog(event.getDemandPublishedRequestedId());
    var seller = fetchSeller(event.getSellerId());
    var demand = fetchDemand(event.getDemandId());
    var now = LocalDateTime.now();

    return JNotificationRequested.builder()
        .id(event.getId())
        .demandPublishedRequested(parent)
        .seller(seller)
        .demand(demand)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .status(ProcessStatus.PENDING)
        .attemptNb(event.getAttemptNb())
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private void updateEventLogStatus(JNotificationRequested eventLog, ProcessStatus status) {
    eventLog.setStatus(status);
    eventLog.setUpdatedAt(LocalDateTime.now());
    notificationRequestedRepository.save(eventLog);
  }

  private JDemandPublishedRequested fetchParentEventLog(String parentId) {
    return demandPublishedRequestedRepository
        .findById(parentId)
        .orElseThrow(
            () -> {
              log.error("Parent event log not found: {}", forJava(parentId));
              return new IllegalStateException("Parent event log not found: " + parentId);
            });
  }

  private JSeller fetchSeller(String sellerId) {
    return userRepository
        .findById(sellerId)
        .filter(user -> user instanceof JSeller)
        .map(user -> (JSeller) user)
        .orElseThrow(
            () -> {
              log.error("Seller not found: {}", forJava(sellerId));
              return new IllegalStateException("Seller not found: " + sellerId);
            });
  }

  private JDemand fetchDemand(String demandId) {
    return demandRepository
        .findByIdWithRelations(demandId)
        .orElseThrow(
            () -> {
              log.error("Demand not found: {}", forJava(demandId));
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
        .sellerId(sellerId)
        .demandId(demand.getId())
        .message(message)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .clickAction(clickAction)
        .build();
  }

  private void handleEventProcessingError(
      JNotificationRequested eventLog, NotificationRequested event, Exception e) {

    log.error(
        "Failed to process NotificationRequested: {}, seller: {}, attempt: {}",
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

    throw new RuntimeException("NotificationRequested processing failed", e);
  }
}
