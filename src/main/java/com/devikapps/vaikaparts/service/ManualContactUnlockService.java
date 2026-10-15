package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.FAILED;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING_MANUAL_REVIEW;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;
import static com.devikapps.vaikaparts.model.classifier.NotificationType.CONTACT_UNLOCKED;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.ContactUnlockResponse;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.ManualContactUnlockAdminResponse;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.repository.ContactUnlockRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JContactUnlock;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManualContactUnlockService {

  private final ContactUnlockRepository repository;
  private final NotificationService notificationService;
  private final UserService userService;

  @Transactional
  public ContactUnlockResponse confirm(
      String unlockRequestId, String paymentReference, String reviewNote) {
    var manager = userService.getCurrentManager();
    JContactUnlock unlock =
        repository
            .findByUnlockRequestIdForUpdate(unlockRequestId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown contact unlock request"));
    if (!"MANUAL_ORANGE_MONEY".equals(unlock.getProvider())) {
      throw new IllegalArgumentException("Contact unlock is not a manual payment");
    }
    if (unlock.getStatus() == RELEASED) {
      return response(unlock);
    }
    if (unlock.getStatus() != PENDING_MANUAL_REVIEW) {
      throw new IllegalStateException("Contact unlock is not pending manual review");
    }

    notifyContact(unlock, true);
    notifyContact(unlock, false);

    var now = OffsetDateTime.now(ZoneOffset.UTC);
    unlock.setStatus(RELEASED);
    unlock.setPaymentId("MANUAL-" + unlock.getUnlockRequestId());
    unlock.setReleaseEventId("MANUAL-" + unlock.getUnlockRequestId());
    unlock.setPaidAt(now);
    unlock.setReleasedAt(now);
    unlock.setReviewedAt(now);
    unlock.setUpdatedAt(now);
    unlock.setReviewedBy(manager.getId());
    unlock.setManualPaymentReference(trimToNull(paymentReference));
    unlock.setReviewNote(trimToNull(reviewNote));
    return response(repository.save(unlock));
  }

  @Transactional(readOnly = true)
  public List<ManualContactUnlockAdminResponse> findPendingReviews() {
    userService.getCurrentManager();
    return repository
        .findByProviderAndStatusOrderByCreatedAtAsc("MANUAL_ORANGE_MONEY", PENDING_MANUAL_REVIEW)
        .stream()
        .map(
            unlock ->
                new ManualContactUnlockAdminResponse(
                    unlock.getUnlockRequestId(),
                    unlock.getOffer().getId(),
                    unlock.getBuyer().getId(),
                    unlock.getBuyer().getName(),
                    unlock.getBuyer().getPhoneNumber(),
                    unlock.getSeller().getId(),
                    unlock.getStatus(),
                    unlock.getCreatedAt()))
        .toList();
  }

  @Transactional
  public ContactUnlockResponse reject(String unlockRequestId, String reviewNote) {
    var manager = userService.getCurrentManager();
    JContactUnlock unlock =
        repository
            .findByUnlockRequestIdForUpdate(unlockRequestId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown contact unlock request"));
    if (!"MANUAL_ORANGE_MONEY".equals(unlock.getProvider())
        || unlock.getStatus() != PENDING_MANUAL_REVIEW) {
      throw new IllegalStateException("Contact unlock is not pending manual review");
    }
    var now = OffsetDateTime.now(ZoneOffset.UTC);
    unlock.setStatus(FAILED);
    unlock.setReviewedBy(manager.getId());
    unlock.setReviewedAt(now);
    unlock.setUpdatedAt(now);
    unlock.setReviewNote(trimToNull(reviewNote));
    return response(repository.save(unlock));
  }

  private void notifyContact(JContactUnlock unlock, boolean notifySeller) {
    var contact = notifySeller ? unlock.getBuyer() : unlock.getSeller();
    var recipient = notifySeller ? unlock.getSeller() : unlock.getBuyer();
    notificationService.createAndSendNotification(
        NotificationRequest.builder()
            .recipientUserId(recipient.getId())
            .resourceId(unlock.getOffer().getId())
            .notificationType(CONTACT_UNLOCKED)
            .message(
                safe(contact.getName())
                    + " — "
                    + safe(contact.getPhoneNumber())
                    + " — "
                    + safe(contact.getEmail()))
            .clickAction("/offers/" + unlock.getOffer().getId())
            .build());
  }

  private ContactUnlockResponse response(JContactUnlock unlock) {
    return new ContactUnlockResponse(unlock.getUnlockRequestId(), unlock.getStatus(), null);
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String safe(String value) {
    return value == null || value.isBlank() ? "-" : value;
  }
}
