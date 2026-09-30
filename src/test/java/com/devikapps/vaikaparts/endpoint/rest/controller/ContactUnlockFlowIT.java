package com.devikapps.vaikaparts.endpoint.rest.controller;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;
import static com.devikapps.vaikaparts.model.classifier.NotificationType.CONTACT_UNLOCKED;
import static com.devikapps.vaikaparts.model.classifier.PostStatus.PUBLISHED;
import static com.devikapps.vaikaparts.model.classifier.UserStatus.ENABLED;
import static com.devikapps.vaikaparts.model.classifier.UserType.RESEARCHER;
import static com.devikapps.vaikaparts.model.classifier.UserType.SELLER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.client.PecuniaClient;
import com.devikapps.vaikaparts.client.PecuniaClient.PaymentResponse;
import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.event.model.ContactReleaseRequested;
import com.devikapps.vaikaparts.repository.ContactUnlockRepository;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JContactUnlock;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.ContactReleaseService;
import com.devikapps.vaikaparts.service.ContactUnlockService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ContactUnlockFlowIT extends FacadeIT {

  @Autowired private ContactUnlockService contactUnlockService;
  @Autowired private ContactReleaseService contactReleaseService;
  @Autowired private ContactUnlockRepository contactUnlockRepository;
  @Autowired private DemandPublishedNotificationRepository notificationRepository;
  @Autowired private OfferRepository offerRepository;
  @Autowired private DemandRepository demandRepository;
  @Autowired private UserRepository userRepository;
  @MockitoBean private PecuniaClient pecuniaClient;

  private JResearcher buyer;
  private JSeller seller;
  private JDemand demand;

  @BeforeEach
  void setUp() {
    var now = OffsetDateTime.now();
    buyer =
        userRepository.save(
            JResearcher.builder()
                .id("flow-buyer")
                .supabaseUserId("flow-buyer-sub")
                .name("Buyer")
                .phoneNumber("+261340000001")
                .email("buyer@example.com")
                .userType(RESEARCHER)
                .status(ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build());
    seller =
        userRepository.save(
            JSeller.builder()
                .id("flow-seller")
                .supabaseUserId("flow-seller-sub")
                .name("Seller")
                .phoneNumber("+261340000002")
                .email("seller@example.com")
                .garageName("Flow Garage")
                .userType(SELLER)
                .status(ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build());
    demand =
        demandRepository.save(
            JDemand.builder()
                .id("flow-demand")
                .researcher(buyer)
                .status(PUBLISHED)
                .createdAt(now.toLocalDateTime())
                .updatedAt(now.toLocalDateTime())
                .build());
    authenticateBuyer();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    notificationRepository.deleteAll();
    contactUnlockRepository.deleteAll();
    offerRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void shouldReleaseContactOnceAcrossTheCompletePaymentFlow() {
    createOffer("flow-offer");
    when(pecuniaClient.initiate(any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation ->
                payment(invocation.getArgument(0), "payment-1", "https://pay.example/flow"));

    var initiated = contactUnlockService.initiate("flow-offer", "VANILLA_PAY");
    var retried = contactUnlockService.initiate("flow-offer", "VANILLA_PAY");

    assertEquals(initiated.unlockRequestId(), retried.unlockRequestId());
    verify(pecuniaClient, times(1)).initiate(any(), any(), any(), any(), any());

    seller.setName("Seller Updated");
    seller.setPhoneNumber("+261340000099");
    seller.setEmail("updated@example.com");
    userRepository.saveAndFlush(seller);
    var event = releaseEvent("event-1", "payment-1", initiated.unlockRequestId());

    contactReleaseService.release(event);
    contactReleaseService.release(event);

    var released = contactUnlockRepository.findById(initiated.unlockRequestId()).orElseThrow();
    assertEquals(RELEASED, released.getStatus());
    var contact = contactUnlockService.getSellerContact("flow-offer");
    assertEquals("Seller Updated", contact.name());
    assertEquals("+261340000099", contact.phoneNumber());
    assertEquals("updated@example.com", contact.email());
    var notifications = notificationRepository.findAll();
    assertEquals(1, notifications.size());
    assertEquals(CONTACT_UNLOCKED, notifications.getFirst().getNotificationType());
    assertEquals("flow-seller", notifications.getFirst().getRecipient().getId());
  }

  @ParameterizedTest
  @ValueSource(ints = {10, 100})
  void shouldReleaseEveryIndependentUnlockWithoutDuplicates(int count) {
    var now = OffsetDateTime.now();
    for (int index = 0; index < count; index++) {
      var offer = createOffer("burst-offer-" + index);
      contactUnlockRepository.saveAndFlush(
          JContactUnlock.builder()
              .unlockRequestId("burst-unlock-" + index)
              .offer(offer)
              .buyer(buyer)
              .seller(seller)
              .provider("VANILLA_PAY")
              .status(PENDING)
              .createdAt(now)
              .updatedAt(now)
              .build());
      var event =
          releaseEvent("burst-event-" + index, "burst-payment-" + index, "burst-unlock-" + index);
      contactReleaseService.release(event);
      contactReleaseService.release(event);
    }

    var unlocks = contactUnlockRepository.findAll();
    assertEquals(count, unlocks.size());
    assertTrue(unlocks.stream().allMatch(unlock -> unlock.getStatus() == RELEASED));
    assertEquals(count, notificationRepository.count());
  }

  private JOffer createOffer(String offerId) {
    var now = OffsetDateTime.now().toLocalDateTime();
    return offerRepository.saveAndFlush(
        JOffer.builder()
            .id(offerId)
            .seller(seller)
            .demand(demand)
            .status(PUBLISHED)
            .createdAt(now)
            .updatedAt(now)
            .build());
  }

  private PaymentResponse payment(String unlockRequestId, String paymentId, String paymentUrl) {
    return new PaymentResponse(
        paymentId,
        "transaction-1",
        paymentUrl,
        "PENDING",
        new BigDecimal("5000"),
        "MGA",
        "Unlock seller contact for offer flow-offer",
        "PROFILE_UNLOCK",
        unlockRequestId,
        buyer.getId(),
        seller.getId(),
        "VANILLA_PAY");
  }

  private ContactReleaseRequested releaseEvent(
      String eventId, String paymentId, String unlockRequestId) {
    return ContactReleaseRequested.builder()
        .id(eventId)
        .paymentId(paymentId)
        .unlockRequestId(unlockRequestId)
        .buyerId(buyer.getId())
        .sellerId(seller.getId())
        .provider("VANILLA_PAY")
        .paidAt(Instant.parse("2026-09-30T10:15:30Z"))
        .build();
  }

  private void authenticateBuyer() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(buyer.getSupabaseUserId(), null, List.of()));
  }
}
