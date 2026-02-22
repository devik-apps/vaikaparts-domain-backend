package com.devikapps.vaikaparts.service.event;

import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.event.model.NotificationRequested;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandPublishedRequestedRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.service.SellerService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandPublishedRequestedService implements Consumer<DemandPublishedRequested> {

  private final DemandPublishedRequestedRepository demandPublishedRequestedRepository;
  private final DemandRepository demandRepository;
  private final SellerService sellerService;
  private final EventProducer<NotificationRequested> notificationRequestedProducer;

  @Override
  @Transactional
  public void accept(DemandPublishedRequested event) {
    log.info(
        "Processing DemandPublishedRequested event: {}, demand: {}, attempt: {}",
        forJava(event.getId()),
        forJava(event.getDemandId()),
        event.getAttemptNb());

    var eventLog = createOrUpdateEventLog(event);

    try {
      updateEventLogStatus(eventLog, ProcessStatus.PROCESSING);

      var demand = fetchDemand(event.getDemandId());
      var sellers = fetchActiveSellers();

      eventLog.setTotalSellersToNotify(sellers.size());
      demandPublishedRequestedRepository.save(eventLog);

      publishNotificationRequests(event, demand, sellers);

      updateEventLogStatus(eventLog, ProcessStatus.SUCCESS);
      eventLog.setNotificationsSentCount(sellers.size());
      eventLog.setCompletedAt(LocalDateTime.now());
      demandPublishedRequestedRepository.save(eventLog);

      log.info(
          "Successfully processed DemandPublishedRequested: {}, notified {} sellers",
          forJava(event.getId()),
          sellers.size());

    } catch (Exception e) {
      handleEventProcessingError(eventLog, event, e);
    }
  }

  private JDemandPublishedRequested createOrUpdateEventLog(DemandPublishedRequested event) {
    return demandPublishedRequestedRepository
        .findById(event.getId())
        .orElseGet(() -> createNewEventLog(event));
  }

  private JDemandPublishedRequested createNewEventLog(DemandPublishedRequested event) {
    var demand = fetchDemand(event.getDemandId());
    var now = LocalDateTime.now();

    return JDemandPublishedRequested.builder()
        .id(event.getId())
        .demand(demand)
        .status(ProcessStatus.PENDING)
        .attemptNb(event.getAttemptNb())
        .totalSellersToNotify(0)
        .notificationsSentCount(0)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private void updateEventLogStatus(JDemandPublishedRequested eventLog, ProcessStatus status) {
    eventLog.setStatus(status);
    eventLog.setUpdatedAt(LocalDateTime.now());
    demandPublishedRequestedRepository.save(eventLog);
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

  private List<Seller> fetchActiveSellers() {
    var sellers = sellerService.getAllActiveSellers();

    log.info("Found {} active sellers to notify", sellers.size());
    return sellers;
  }

  private void publishNotificationRequests(
      DemandPublishedRequested parentEvent, JDemand demand, List<Seller> sellers) {

    var notificationEvents = new ArrayList<NotificationRequested>();

    for (Seller seller : sellers) {
      var notificationEvent =
          NotificationRequested.builder()
              .id(randomUUID().toString())
              .demandPublishedRequestedId(parentEvent.getId())
              .sellerId(seller.getId())
              .demandId(demand.getId())
              .build();

      notificationEvents.add(notificationEvent);
    }

    if (!notificationEvents.isEmpty()) {
      notificationRequestedProducer.accept(notificationEvents);
      log.info("Published {} NotificationRequested events", notificationEvents.size());
    }
  }

  private void handleEventProcessingError(
      JDemandPublishedRequested eventLog, DemandPublishedRequested event, Exception e) {

    log.error(
        "Failed to process DemandPublishedRequested: {}, attempt: {}",
        forJava(event.getId()),
        event.getAttemptNb(),
        e);

    eventLog.setStatus(ProcessStatus.FAILED);
    eventLog.setAttemptNb(event.getAttemptNb());
    eventLog.setErrorMessage(e.getMessage());
    eventLog.setUpdatedAt(LocalDateTime.now());
    eventLog.setCompletedAt(LocalDateTime.now());
    demandPublishedRequestedRepository.save(eventLog);

    throw new RuntimeException("DemandPublishedRequested processing failed", e);
  }
}
