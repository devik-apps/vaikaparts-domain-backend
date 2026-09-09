package com.devikapps.vaikaparts.service.event;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.event.model.OfferNotificationRequested;
import com.devikapps.vaikaparts.exception.OfferNotificationRequestedException;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.event.JNotificationRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
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
public class OfferNotificationRequestedService implements Consumer<OfferNotificationRequested> {

  private final NotificationRequestedRepository notificationRequestedRepository;
  private final OfferRepository offerRepository;
  private final NotificationService notificationService;

  @Override
  @Transactional
  public void accept(OfferNotificationRequested event) {
    log.info(
        "Processing OfferNotificationRequested event={}, offer={}, recipientType=RESEARCHER,"
            + " recipientId={}, attempt={}",
        forJava(event.getId()),
        forJava(event.getOfferId()),
        forJava(event.getResearcherId()),
        event.getAttemptNb());

    var eventLog = createOrUpdateEventLog(event);
    if (eventLog.getStatus() == ProcessStatus.SUCCESS) {
      log.info("Skipping completed OfferNotificationRequested event={}", forJava(event.getId()));
      return;
    }

    try {
      eventLog.setAttemptNb(event.getAttemptNb());
      eventLog.setErrorMessage(null);
      eventLog.setCompletedAt(null);
      updateEventLogStatus(eventLog, ProcessStatus.PROCESSING);

      var request = buildNotificationRequest(eventLog.getOffer(), event.getResearcherId());
      request.setNotificationRequestedId(event.getId());
      notificationService.createAndSendNotification(request);

      eventLog.setCompletedAt(LocalDateTime.now());
      updateEventLogStatus(eventLog, ProcessStatus.SUCCESS);
      log.info(
          "Processed OfferNotificationRequested event={}, recipientType=RESEARCHER, recipientId={}",
          forJava(event.getId()),
          forJava(event.getResearcherId()));
    } catch (Exception e) {
      handleEventProcessingError(eventLog, event, e);
    }
  }

  private JNotificationRequested createOrUpdateEventLog(OfferNotificationRequested event) {
    var existing = notificationRequestedRepository.findById(event.getId());
    if (existing.isPresent()) {
      var eventLog = existing.get();
      if (eventLog.getOffer() == null
          || eventLog.getResearcher() == null
          || !eventLog.getOffer().getId().equals(event.getOfferId())
          || !eventLog.getResearcher().getId().equals(event.getResearcherId())) {
        throw new IllegalArgumentException(
            "Event id already belongs to another notification request");
      }
      return eventLog;
    }

    var offer =
        offerRepository
            .findByIdWithRelations(event.getOfferId())
            .orElseThrow(() -> new IllegalStateException("Offer not found: " + event.getOfferId()));
    var researcher = offer.getDemand().getResearcher();
    if (!researcher.getId().equals(event.getResearcherId())) {
      throw new IllegalArgumentException("Notification recipient must own the offer's demand");
    }
    var now = LocalDateTime.now();
    return JNotificationRequested.builder()
        .id(event.getId())
        .researcher(researcher)
        .offer(offer)
        .notificationType(NotificationType.OFFER_PUBLISHED)
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

  private NotificationRequest buildNotificationRequest(JOffer offer, String researcherId) {
    var part = offer.getDemand().getPart();
    var message =
        format(
            "New offer: %s %s %s (%s)",
            part.getCarBrand(), part.getCarModel(), part.getPartName(), part.getCarYear());
    var clickAction =
        format(
            "{\"action\":\"VIEW_OFFER\",\"offerId\":\"%s\",\"demandId\":\"%s\"}",
            offer.getId(), offer.getDemand().getId());
    return NotificationRequest.builder()
        .recipientUserId(researcherId)
        .resourceId(offer.getId())
        .notificationType(NotificationType.OFFER_PUBLISHED)
        .message(message)
        .clickAction(clickAction)
        .build();
  }

  private void handleEventProcessingError(
      JNotificationRequested eventLog, OfferNotificationRequested event, Exception e) {
    log.error(
        "Failed OfferNotificationRequested event={}, recipientType=RESEARCHER, recipientId={},"
            + " attempt={}",
        forJava(event.getId()),
        forJava(event.getResearcherId()),
        event.getAttemptNb(),
        e);
    eventLog.setErrorMessage(e.getMessage());
    eventLog.setCompletedAt(LocalDateTime.now());
    updateEventLogStatus(eventLog, ProcessStatus.FAILED);
    throw new OfferNotificationRequestedException("Offer notification processing failed", e);
  }
}
