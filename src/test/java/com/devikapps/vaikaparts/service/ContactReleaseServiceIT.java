package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;
import static com.devikapps.vaikaparts.model.classifier.NotificationType.CONTACT_UNLOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.event.model.ContactReleaseRequested;
import com.devikapps.vaikaparts.repository.ContactUnlockRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JContactUnlock;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactReleaseServiceIT {

  @Mock private ContactUnlockRepository repository;
  @Mock private NotificationService notificationService;

  private ContactReleaseService service;
  private JContactUnlock unlock;

  @BeforeEach
  void setUp() {
    var buyer =
        JResearcher.builder()
            .id("buyer-id")
            .name("Alice")
            .phoneNumber("+261340000001")
            .email("alice@example.com")
            .build();
    var seller = JSeller.builder().id("seller-id").build();
    var offer = JOffer.builder().id("offer-id").seller(seller).build();
    unlock =
        JContactUnlock.builder()
            .unlockRequestId("unlock-id")
            .buyer(buyer)
            .seller(seller)
            .offer(offer)
            .provider("VANILLA_PAY")
            .status(PENDING)
            .build();
    service = new ContactReleaseService(repository, notificationService);
  }

  @Test
  void shouldReleaseAndNotifySellerWithCurrentBuyerContacts() {
    var event = event("event-id", "payment-id", "buyer-id", "seller-id", "VANILLA_PAY");
    when(repository.findByUnlockRequestIdForUpdate("unlock-id")).thenReturn(Optional.of(unlock));
    when(repository.save(unlock)).thenReturn(unlock);

    service.release(event);

    assertEquals(RELEASED, unlock.getStatus());
    assertEquals("payment-id", unlock.getPaymentId());
    assertEquals("event-id", unlock.getReleaseEventId());
    assertEquals(event.getPaidAt(), unlock.getPaidAt().toInstant());
    var request = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(notificationService).createAndSendNotification(request.capture());
    assertEquals(CONTACT_UNLOCKED, request.getValue().getNotificationType());
    assertEquals("seller-id", request.getValue().getRecipientUserId());
    assertEquals("offer-id", request.getValue().getResourceId());
    assertEquals("Alice — +261340000001 — alice@example.com", request.getValue().getMessage());
  }

  @Test
  void shouldIgnoreAnIdenticalRedelivery() {
    unlock.setStatus(RELEASED);
    unlock.setPaymentId("payment-id");
    unlock.setReleaseEventId("event-id");
    when(repository.findByUnlockRequestIdForUpdate("unlock-id")).thenReturn(Optional.of(unlock));

    service.release(event("event-id", "payment-id", "buyer-id", "seller-id", "VANILLA_PAY"));

    verify(notificationService, never()).createAndSendNotification(any());
    verify(repository, never()).save(any());
  }

  @Test
  void shouldRejectEveryCorrelationMismatch() {
    when(repository.findByUnlockRequestIdForUpdate("unlock-id")).thenReturn(Optional.of(unlock));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.release(event("event-id", "payment-id", "other", "seller-id", "VANILLA_PAY")));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.release(event("event-id", "payment-id", "buyer-id", "other", "VANILLA_PAY")));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.release(event("event-id", "payment-id", "buyer-id", "seller-id", "OTHER")));
  }

  @Test
  void shouldNotPersistReleaseWhenNotificationFails() {
    when(repository.findByUnlockRequestIdForUpdate("unlock-id")).thenReturn(Optional.of(unlock));
    when(notificationService.createAndSendNotification(any()))
        .thenThrow(new IllegalStateException("notification persistence failed"));

    assertThrows(
        IllegalStateException.class,
        () ->
            service.release(
                event("event-id", "payment-id", "buyer-id", "seller-id", "VANILLA_PAY")));

    verify(repository, never()).save(any());
  }

  private ContactReleaseRequested event(
      String eventId, String paymentId, String buyerId, String sellerId, String provider) {
    return ContactReleaseRequested.builder()
        .id(eventId)
        .paymentId(paymentId)
        .unlockRequestId("unlock-id")
        .buyerId(buyerId)
        .sellerId(sellerId)
        .provider(provider)
        .paidAt(Instant.parse("2026-09-30T10:15:30Z"))
        .build();
  }
}
