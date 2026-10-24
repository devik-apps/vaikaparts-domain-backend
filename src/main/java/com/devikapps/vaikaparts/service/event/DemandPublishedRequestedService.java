package com.devikapps.vaikaparts.service.event;

import static com.devikapps.vaikaparts.model.classifier.UserStatus.ENABLED;
import static com.devikapps.vaikaparts.model.classifier.UserType.SELLER;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.event.model.DemandPublishedNotificationRequested;
import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.exception.DemandPublishedRequestedException;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandPublishedRequestedRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotificationRequested;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.outbox.OutboxService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
  private final OfferRepository offerRepository;
  private final UserRepository userRepository;
  private final SellerMapper sellerMapper;
  private final NotificationRequestedRepository notificationRequestedRepository;
  private final OutboxService outboxService;

  @Override
  @Transactional
  public void accept(DemandPublishedRequested event) {
    log.info(
        "[NOTIF-PIPELINE][BATCH] Processing DemandPublishedRequested event: {}, demand: {},"
            + " attempt: {}",
        forJava(event.getId()),
        forJava(event.getDemandId()),
        event.getAttemptNb());

    var demand = fetchDemand(event.getDemandId());
    log.info("[NOTIF-PIPELINE][BATCH] Demand resolved: eventId={}", forJava(event.getId()));
    var eventLog = fetchOrCreateEventLog(event, demand);
    log.info(
        "[NOTIF-PIPELINE][BATCH] Parent log resolved: eventId={}, status={}",
        forJava(event.getId()),
        eventLog.getStatus());

    if (eventLog.getStatus() == ProcessStatus.SUCCESS) {
      log.info(
          "[NOTIF-PIPELINE][BATCH] Ignoring completed parent event {}", forJava(event.getId()));
      return;
    }

    try {
      updateEventLogStatus(eventLog);

      // Historical counter name retained; for an offer the recipient is one researcher.
      var sellers =
          event.getOfferId() == null
              ? matchSellerToPartCategory(
                  fetchActiveSellers(),
                  demand.getPart() == null ? null : demand.getPart().getPartCategory())
              : List.<Seller>of();

      int recipientCount = event.getOfferId() == null ? sellers.size() : 1;
      eventLog.setTotalSellersToNotify(recipientCount);
      demandPublishedRequestedRepository.save(eventLog);

      publishNotificationRequests(event, eventLog, demand, sellers);

      eventLog.setStatus(ProcessStatus.SUCCESS);
      eventLog.setAttemptNb(event.getAttemptNb());
      eventLog.setErrorMessage(null);
      eventLog.setNotificationsSentCount(recipientCount);
      eventLog.setCompletedAt(LocalDateTime.now());
      eventLog.setUpdatedAt(LocalDateTime.now());
      demandPublishedRequestedRepository.save(eventLog);

      log.info(
          "[NOTIF-PIPELINE][BATCH] Handler finishing: eventId={}, scheduledRecipients={} (not"
              + " delivery confirmation)",
          forJava(event.getId()),
          recipientCount);

    } catch (Exception e) {
      handleEventProcessingError(eventLog, event, e);
    }
  }

  private List<Seller> matchSellerToPartCategory(
      List<Seller> activeSellers, PartCategory partCategory) {
    var matchingSellers =
        activeSellers.stream()
            .filter(
                seller ->
                    Boolean.TRUE.equals(seller.getHandleAllCategory())
                        || (partCategory != null
                            && seller.getCategoryList() != null
                            && seller.getCategoryList().contains(partCategory)))
            .toList();
    log.info(
        "[NOTIF-PIPELINE][CATEGORY_FILTER] recipientType=SELLER, partCategory={},"
            + " activeSellers={}, matchingSellers={}",
        partCategory,
        activeSellers.size(),
        matchingSellers.size());
    return matchingSellers;
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
              log.error("[NOTIF-PIPELINE][BATCH] Demand not found: {}", forJava(demandId));
              return new IllegalStateException("Demand not found: " + demandId);
            });
  }

  private List<Seller> fetchActiveSellers() {
    var sellers =
        userRepository.findAllByUserTypeAndStatus(SELLER, ENABLED).stream()
            .map(u -> sellerMapper.toSeller((JSeller) u))
            .toList();

    log.info(
        "[NOTIF-PIPELINE][BATCH] Found {} active sellers before category filtering",
        sellers.size());
    return sellers;
  }

  private void publishNotificationRequests(
      DemandPublishedRequested parentEvent,
      JDemandPublishedRequested parentLog,
      JDemand demand,
      List<Seller> sellers) {

    if (sellers.isEmpty() && parentEvent.getOfferId() == null) {
      log.info("[NOTIF-PIPELINE][BATCH] No recipients: parentId={}", forJava(parentEvent.getId()));
      return;
    }

    if (parentEvent.getOfferId() != null) {
      var offer =
          offerRepository
              .findById(parentEvent.getOfferId())
              .orElseThrow(
                  () -> new IllegalStateException("Offer not found: " + parentEvent.getOfferId()));
      if (!demand.getId().equals(offer.getDemand().getId())) {
        throw new IllegalArgumentException("Offer does not belong to the event demand");
      }
      var childId = stableChildId(parentEvent.getId(), "offer", offer.getId());
      var notificationEvent =
          DemandPublishedNotificationRequested.builder()
              .id(childId)
              .demandPublishedRequestedId(parentEvent.getId())
              .demandId(demand.getId())
              .offerId(offer.getId())
              .researcherId(demand.getResearcher().getId())
              .build();
      persistChildIfAbsent(
          notificationEvent,
          JDemandPublishedNotificationRequested.builder()
              .id(childId)
              .demandPublishedRequested(parentLog)
              .offer(offer)
              .researcher(demand.getResearcher())
              .notificationType(
                  com.devikapps.vaikaparts.model.classifier.NotificationType.OFFER_PUBLISHED)
              .build());
      log.info(
          "[NOTIF-PIPELINE][BATCH] Offer child prepared: parentId={}, offerId={},"
              + " recipientType=RESEARCHER, recipientId={}",
          forJava(parentEvent.getId()),
          forJava(offer.getId()),
          forJava(demand.getResearcher().getId()));
    } else {
      for (var seller : sellers) {
        var childId = stableChildId(parentEvent.getId(), "seller", seller.getId());
        var notificationEvent =
            DemandPublishedNotificationRequested.builder()
                .id(childId)
                .demandPublishedRequestedId(parentEvent.getId())
                .sellerId(seller.getId())
                .demandId(demand.getId())
                .build();
        persistChildIfAbsent(
            notificationEvent,
            JDemandPublishedNotificationRequested.builder()
                .id(childId)
                .demandPublishedRequested(parentLog)
                .seller(sellerMapper.toPersistence(seller))
                .demand(demand)
                .notificationType(
                    com.devikapps.vaikaparts.model.classifier.NotificationType.DEMAND_PUBLISHED)
                .build());
      }
    }
  }

  private void persistChildIfAbsent(
      DemandPublishedNotificationRequested event, JDemandPublishedNotificationRequested childLog) {
    if (notificationRequestedRepository.existsById(event.getId())) {
      return;
    }
    var now = LocalDateTime.now();
    childLog.setStatus(ProcessStatus.PENDING);
    childLog.setAttemptNb(0);
    childLog.setCreatedAt(now);
    childLog.setUpdatedAt(now);
    notificationRequestedRepository.save(childLog);
    outboxService.enqueue(event);
  }

  private String stableChildId(String parentId, String recipientType, String recipientId) {
    return UUID.nameUUIDFromBytes(
            (parentId + ":" + recipientType + ":" + recipientId).getBytes(StandardCharsets.UTF_8))
        .toString();
  }

  private void handleEventProcessingError(
      JDemandPublishedRequested eventLog, DemandPublishedRequested event, Exception e) {

    log.error(
        "[NOTIF-PIPELINE][BATCH] Failed to process DemandPublishedRequested: {}, attempt: {}",
        forJava(event.getId()),
        event.getAttemptNb(),
        e);

    eventLog.setStatus(ProcessStatus.FAILED);
    eventLog.setAttemptNb(event.getAttemptNb());
    eventLog.setErrorMessage(e.getMessage());
    eventLog.setUpdatedAt(LocalDateTime.now());
    eventLog.setCompletedAt(LocalDateTime.now());
    demandPublishedRequestedRepository.save(eventLog);

    throw new DemandPublishedRequestedException("DemandPublishedRequested processing failed", e);
  }
}
