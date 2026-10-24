package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.FAILED;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING_MANUAL_REVIEW;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;
import static com.devikapps.vaikaparts.model.classifier.NotificationType.CONTACT_UNLOCKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.repository.ContactUnlockRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JContactUnlock;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManualContactUnlockServiceTest {

  @Mock private ContactUnlockRepository repository;
  @Mock private NotificationService notificationService;
  @Mock private UserService userService;

  private ManualContactUnlockService service;
  private JContactUnlock unlock;

  @BeforeEach
  void setUp() {
    var buyer =
        JResearcher.builder()
            .id("buyer-id")
            .name("Buyer")
            .phoneNumber("0321111111")
            .email("buyer@example.com")
            .build();
    var seller =
        JSeller.builder()
            .id("seller-id")
            .name("Seller")
            .phoneNumber("0322222222")
            .email("seller@example.com")
            .build();
    unlock =
        JContactUnlock.builder()
            .unlockRequestId("unlock-id")
            .buyer(buyer)
            .seller(seller)
            .offer(JOffer.builder().id("offer-id").seller(seller).build())
            .provider("MANUAL_ORANGE_MONEY")
            .status(PENDING_MANUAL_REVIEW)
            .build();
    service = new ManualContactUnlockService(repository, notificationService, userService);
    when(userService.getCurrentManager()).thenReturn(JManager.builder().id("manager-id").build());
  }

  @Test
  void shouldConfirmOnceAuditAndNotifyBothParties() {
    when(repository.findByUnlockRequestIdForUpdate("unlock-id")).thenReturn(Optional.of(unlock));
    when(repository.save(unlock)).thenReturn(unlock);

    var result = service.confirm("unlock-id", "TX-OPTIONAL", "Vu sur Orange Money");

    assertEquals(RELEASED, result.status());
    assertEquals("manager-id", unlock.getReviewedBy());
    assertEquals("TX-OPTIONAL", unlock.getManualPaymentReference());
    assertEquals("Vu sur Orange Money", unlock.getReviewNote());
    var requests = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(notificationService, times(2)).createAndSendNotification(requests.capture());
    assertEquals(CONTACT_UNLOCKED, requests.getAllValues().get(0).getNotificationType());
    assertEquals("seller-id", requests.getAllValues().get(0).getRecipientUserId());
    assertEquals(
        "Buyer — 0321111111 — buyer@example.com", requests.getAllValues().get(0).getMessage());
    assertEquals("buyer-id", requests.getAllValues().get(1).getRecipientUserId());
    assertEquals(
        "Seller — 0322222222 — seller@example.com", requests.getAllValues().get(1).getMessage());
  }

  @Test
  void shouldIgnoreRepeatedConfirmation() {
    unlock.setStatus(RELEASED);
    unlock.setReviewedBy("manager-id");
    when(repository.findByUnlockRequestIdForUpdate("unlock-id")).thenReturn(Optional.of(unlock));

    var result = service.confirm("unlock-id", null, null);

    assertEquals(RELEASED, result.status());
    verify(notificationService, never()).createAndSendNotification(any());
    verify(repository, never()).save(any());
  }

  @Test
  void shouldListPendingReviewsWithBuyerAndOfferDetails() {
    when(repository.findByProviderAndStatusOrderByCreatedAtAsc(
            "MANUAL_ORANGE_MONEY", PENDING_MANUAL_REVIEW))
        .thenReturn(List.of(unlock));

    var result = service.findPendingReviews();

    assertEquals(1, result.size());
    assertEquals("unlock-id", result.getFirst().unlockRequestId());
    assertEquals("offer-id", result.getFirst().offerId());
    assertEquals("Buyer", result.getFirst().buyerName());
    assertEquals("0321111111", result.getFirst().buyerPhoneNumber());
  }

  @Test
  void shouldRejectPendingReviewWithoutNotifyingContacts() {
    when(repository.findByUnlockRequestIdForUpdate("unlock-id")).thenReturn(Optional.of(unlock));
    when(repository.save(unlock)).thenReturn(unlock);

    var result = service.reject("unlock-id", "Transaction introuvable");

    assertEquals(FAILED, result.status());
    assertEquals("manager-id", unlock.getReviewedBy());
    assertEquals("Transaction introuvable", unlock.getReviewNote());
    verify(notificationService, never()).createAndSendNotification(any());
  }
}
