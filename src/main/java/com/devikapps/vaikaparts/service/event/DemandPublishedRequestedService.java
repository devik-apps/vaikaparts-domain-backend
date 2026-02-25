package com.devikapps.vaikaparts.service.event;

import static com.devikapps.vaikaparts.model.classifier.UserStatus.ENABLED;
import static com.devikapps.vaikaparts.model.classifier.UserType.SELLER;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.event.model.NotificationRequested;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandPublishedRequestedRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandPublishedRequestedService implements Consumer<DemandPublishedRequested> {

  private final DemandPublishedRequestedRepository demandPublishedRequestedRepository;
  private final DemandRepository demandRepository;
  private final UserRepository userRepository;
  private final SellerMapper sellerMapper;
  private final EventProducer<NotificationRequested> notificationRequestedProducer;

  @Override
  @Transactional
  public void accept(DemandPublishedRequested event) {
    log.info(
        "Processing DemandPublishedRequested event: {}, demand: {}, attempt: {}",
        forJava(event.getId()),
        forJava(event.getDemandId()),
        event.getAttemptNb());

    var demand = fetchDemand(event.getDemandId());
    var eventLog = fetchOrCreateEventLog(event, demand);

    try {
      updateEventLogStatus(eventLog);

      var sellers = fetchActiveSellers();
      eventLog.setTotalSellersToNotify(sellers.size());
      demandPublishedRequestedRepository.save(eventLog);

      publishNotificationRequests(event, demand, sellers);

      eventLog.setStatus(ProcessStatus.SUCCESS);
      eventLog.setAttemptNb(event.getAttemptNb());
      eventLog.setErrorMessage(null);
      eventLog.setNotificationsSentCount(sellers.size());
      eventLog.setCompletedAt(LocalDateTime.now());
      eventLog.setUpdatedAt(LocalDateTime.now());
      demandPublishedRequestedRepository.save(eventLog);

      log.info(
          "Successfully processed DemandPublishedRequested: {}, notified {} sellers",
          forJava(event.getId()),
          sellers.size());

    } catch (Exception e) {
      handleEventProcessingError(eventLog, event, e);
    }
  }

  private JDemandPublishedRequested fetchOrCreateEventLog(
      DemandPublishedRequested event, JDemand demand) {
    return demandPublishedRequestedRepository
        .findById(event.getId())
        .orElseGet(
            () -> {
              var now = LocalDateTime.now();
              var newLog =
                  JDemandPublishedRequested.builder()
                      .id(event.getId())
                      .demand(demand)
                      .status(ProcessStatus.PENDING)
                      .attemptNb(event.getAttemptNb())
                      .totalSellersToNotify(0)
                      .notificationsSentCount(0)
                      .createdAt(now)
                      .updatedAt(now)
                      .build();
              return demandPublishedRequestedRepository.save(newLog);
            });
  }

  private JDemandPublishedRequested fetchOrCreateEventLog(DemandPublishedRequested event) {
    return demandPublishedRequestedRepository
        .findById(event.getId())
        .orElseGet(
            () -> {
              var now = LocalDateTime.now();
              var newLog =
                  JDemandPublishedRequested.builder()
                      .id(event.getId())
                      .status(ProcessStatus.PENDING)
                      .attemptNb(event.getAttemptNb())
                      .totalSellersToNotify(0)
                      .notificationsSentCount(0)
                      .createdAt(now)
                      .updatedAt(now)
                      .build();
              return demandPublishedRequestedRepository.save(newLog);
            });
  }

  private void updateEventLogStatus(JDemandPublishedRequested eventLog) {
    eventLog.setStatus(ProcessStatus.PROCESSING);
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
    var sellers =
        userRepository.findAllByUserTypeAndStatus(SELLER, ENABLED).stream()
            .map(u -> sellerMapper.toSeller((JSeller) u))
            .toList();

    log.info("Found {} active sellers to notify", sellers.size());
    return sellers;
  }

  private void publishNotificationRequests(
      DemandPublishedRequested parentEvent, JDemand demand, List<Seller> sellers) {

    if (sellers.isEmpty()) {
      return;
    }

    var notificationEvents =
        sellers.stream()
            .map(
                seller ->
                    NotificationRequested.builder()
                        .id(randomUUID().toString())
                        .demandPublishedRequestedId(parentEvent.getId())
                        .sellerId(seller.getId())
                        .demandId(demand.getId())
                        .build())
            .toList();

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              notificationRequestedProducer.accept(notificationEvents);
              log.info(
                  "Published {} NotificationRequested events after transaction commit",
                  notificationEvents.size());
            }
          });
    } else {
      log.warn(
          "No active transaction synchronization. Publishing {} NotificationRequested events"
              + " immediately.",
          notificationEvents.size());
      notificationRequestedProducer.accept(notificationEvents);
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
