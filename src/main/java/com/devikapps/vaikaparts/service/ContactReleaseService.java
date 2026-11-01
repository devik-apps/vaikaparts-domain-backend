package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;
import static com.devikapps.vaikaparts.model.classifier.NotificationType.CONTACT_UNLOCKED;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.event.model.ContactReleaseRequested;
import com.devikapps.vaikaparts.repository.ContactUnlockRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JContactUnlock;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactReleaseService {

  private final ContactUnlockRepository repository;
  private final NotificationService notificationService;

  @Transactional
  public void release(ContactReleaseRequested event) {
    validateRequiredFields(event);
    JContactUnlock unlock =
        repository
            .findByUnlockRequestIdForUpdate(event.getUnlockRequestId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown contact unlock request"));

    validateCorrelation(unlock, event);
    if (unlock.getStatus() == RELEASED) {
      if (event.getPaymentId().equals(unlock.getPaymentId())
          && event.getId().equals(unlock.getReleaseEventId())) {
        return;
      }
      throw new IllegalArgumentException("Conflicting release event for completed unlock");
    }
    if (unlock.getPaymentId() != null && !unlock.getPaymentId().equals(event.getPaymentId())) {
      throw new IllegalArgumentException("Payment does not match the reserved unlock");
    }

    releaseContact(unlock, event.getPaymentId(), event.getId(), event.getPaidAt());
  }

  @Transactional
  public void releasePapi(String unlockRequestId, String paymentId) {
    if (unlockRequestId == null
        || unlockRequestId.isBlank()
        || paymentId == null
        || paymentId.isBlank()) {
      throw new IllegalArgumentException("Incomplete PAPI contact release");
    }
    var unlock =
        repository
            .findByUnlockRequestIdForUpdate(unlockRequestId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown contact unlock request"));
    if (!"PAPI".equals(unlock.getProvider())) {
      throw new IllegalArgumentException("Contact unlock provider mismatch");
    }
    var releaseEventId = "PAPI:" + paymentId;
    if (unlock.getStatus() == RELEASED) {
      if (paymentId.equals(unlock.getPaymentId())
          && releaseEventId.equals(unlock.getReleaseEventId())) {
        return;
      }
      throw new IllegalArgumentException("Conflicting release event for completed unlock");
    }
    if (unlock.getStatus()
        != com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING) {
      throw new IllegalArgumentException("PAPI contact unlock is not pending");
    }
    if (unlock.getPaymentId() != null && !unlock.getPaymentId().equals(paymentId)) {
      throw new IllegalArgumentException("Payment does not match the reserved unlock");
    }

    releaseContact(unlock, paymentId, releaseEventId, Instant.now());
  }

  private void releaseContact(
      JContactUnlock unlock, String paymentId, String releaseEventId, Instant paidAt) {
    var buyer = unlock.getBuyer();
    var message =
        safe(buyer.getName())
            + " — "
            + safe(buyer.getPhoneNumber())
            + " — "
            + safe(buyer.getEmail());
    notificationService.createAndSendNotification(
        NotificationRequest.builder()
            .recipientUserId(unlock.getSeller().getId())
            .resourceId(unlock.getOffer().getId())
            .notificationType(CONTACT_UNLOCKED)
            .message(message)
            .clickAction("/offers/" + unlock.getOffer().getId())
            .build());

    var now = OffsetDateTime.now(ZoneOffset.UTC);
    unlock.setPaymentId(paymentId);
    unlock.setReleaseEventId(releaseEventId);
    unlock.setPaidAt(paidAt.atOffset(ZoneOffset.UTC));
    unlock.setReleasedAt(now);
    unlock.setUpdatedAt(now);
    unlock.setStatus(RELEASED);
    repository.save(unlock);
  }

  private void validateRequiredFields(ContactReleaseRequested event) {
    if (event == null
        || blank(event.getId())
        || blank(event.getPaymentId())
        || blank(event.getUnlockRequestId())
        || blank(event.getBuyerId())
        || blank(event.getSellerId())
        || event.getPaidAt() == null) {
      throw new IllegalArgumentException("Incomplete contact release event");
    }
  }

  private void validateCorrelation(JContactUnlock unlock, ContactReleaseRequested event) {
    if (!"VANILLA_PAY".equals(event.getProvider())
        || !event.getProvider().equals(unlock.getProvider())
        || !event.getBuyerId().equals(unlock.getBuyer().getId())
        || !event.getSellerId().equals(unlock.getSeller().getId())) {
      throw new IllegalArgumentException("Contact release correlation mismatch");
    }
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private String safe(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
