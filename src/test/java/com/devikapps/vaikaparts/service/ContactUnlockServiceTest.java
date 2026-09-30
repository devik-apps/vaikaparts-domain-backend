package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;
import static com.devikapps.vaikaparts.model.classifier.PostStatus.PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.client.PecuniaClient;
import com.devikapps.vaikaparts.client.PecuniaClient.PaymentResponse;
import com.devikapps.vaikaparts.client.PecuniaUnknownOutcomeException;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JContactUnlock;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ContactUnlockServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private OfferRepository offerRepository;
  @Mock private ContactUnlockPersistenceService persistence;
  @Mock private PecuniaClient pecuniaClient;

  private ContactUnlockService service;
  private JResearcher buyer;
  private JOffer offer;

  @BeforeEach
  void setUp() {
    buyer = JResearcher.builder().id("buyer-id").supabaseUserId("supabase-id").build();
    var seller = JSeller.builder().id("seller-id").build();
    var demand = JDemand.builder().id("demand-id").researcher(buyer).build();
    offer =
        JOffer.builder()
            .id("offer-id")
            .status(PUBLISHED)
            .seller(seller)
            .demand(demand)
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("supabase-id", null, java.util.List.of()));
    service =
        new ContactUnlockService(
            userRepository,
            offerRepository,
            persistence,
            pecuniaClient,
            new BigDecimal("5000"));
    lenient().when(userRepository.findBySupabaseUserId("supabase-id")).thenReturn(Optional.of(buyer));
    lenient().when(offerRepository.findByIdWithRelations("offer-id")).thenReturn(Optional.of(offer));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldReservePendingUnlockBeforeCallingPecunia() {
    when(persistence.findActive("buyer-id", "offer-id")).thenReturn(Optional.empty());
    when(persistence.reserve(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(pecuniaClient.initiate(any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation ->
                payment(
                    invocation.getArgument(0),
                    "buyer-id",
                    "seller-id",
                    "payment-id",
                    "https://pay.example/1"));
    when(persistence.attachPayment(any(), any())).thenAnswer(this::attachPayment);

    var result = service.initiate("offer-id", "VANILLA_PAY");

    var order = inOrder(persistence, pecuniaClient);
    order.verify(persistence).reserve(any());
    order.verify(pecuniaClient).initiate(any(), any(), any(), any(), any());
    assertEquals(PENDING, result.status());
    assertEquals("https://pay.example/1", result.paymentUrl());
  }

  @Test
  void shouldReturnExistingActiveUnlockWithoutAnotherInitiation() {
    var existing =
        JContactUnlock.builder()
            .unlockRequestId("unlock-id")
            .status(PENDING)
            .paymentUrl("https://pay.example/existing")
            .build();
    when(persistence.findActive("buyer-id", "offer-id")).thenReturn(Optional.of(existing));

    var result = service.initiate("offer-id", "VANILLA_PAY");

    assertEquals("unlock-id", result.unlockRequestId());
    assertEquals("https://pay.example/existing", result.paymentUrl());
    verify(pecuniaClient, never()).initiate(any(), any(), any(), any(), any());
  }

  @Test
  void shouldRecoverPaymentAfterUnknownInitiationOutcome() {
    when(persistence.findActive("buyer-id", "offer-id")).thenReturn(Optional.empty());
    when(persistence.reserve(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(pecuniaClient.initiate(any(), any(), any(), any(), any()))
        .thenThrow(new PecuniaUnknownOutcomeException("timeout"));
    when(pecuniaClient.findByUnlockRequestId(any()))
        .thenAnswer(
            invocation ->
                payment(
                    invocation.getArgument(0),
                    "buyer-id",
                    "seller-id",
                    "payment-id",
                    "https://pay.example/recovered"));
    when(persistence.attachPayment(any(), any())).thenAnswer(this::attachPayment);

    var result = service.initiate("offer-id", "VANILLA_PAY");

    assertEquals("https://pay.example/recovered", result.paymentUrl());
    verify(pecuniaClient).findByUnlockRequestId(result.unlockRequestId());
  }

  @Test
  void shouldRejectMismatchedPecuniaCorrelation() {
    when(persistence.findActive("buyer-id", "offer-id")).thenReturn(Optional.empty());
    when(persistence.reserve(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(pecuniaClient.initiate(any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation ->
                payment(
                    invocation.getArgument(0),
                    "another-buyer",
                    "seller-id",
                    "payment-id",
                    "https://pay.example/1"));

    assertThrows(IllegalStateException.class, () -> service.initiate("offer-id", "VANILLA_PAY"));
    verify(persistence, never()).attachPayment(any(), any());
  }

  @Test
  void shouldNotRegressReleasedUnlockWhenLateHttpResponseArrives() {
    var released = JContactUnlock.builder().unlockRequestId("unlock-id").status(RELEASED).build();
    var response = payment("unlock-id", "buyer-id", "seller-id", "payment-id", "https://pay.example/1");
    var repository = org.mockito.Mockito.mock(com.devikapps.vaikaparts.repository.ContactUnlockRepository.class);
    when(repository.findByUnlockRequestIdForUpdate("unlock-id")).thenReturn(Optional.of(released));
    when(repository.save(released)).thenReturn(released);

    var updated = new ContactUnlockPersistenceService(repository).attachPayment(released, response);

    assertEquals(RELEASED, updated.getStatus());
    assertEquals("https://pay.example/1", updated.getPaymentUrl());
  }

  private JContactUnlock attachPayment(org.mockito.invocation.InvocationOnMock invocation) {
    JContactUnlock unlock = invocation.getArgument(0);
    PaymentResponse payment = invocation.getArgument(1);
    unlock.setPaymentId(payment.paymentId());
    unlock.setPaymentUrl(payment.paymentUrl());
    return unlock;
  }

  private PaymentResponse payment(
      String unlockId, String buyerId, String sellerId, String paymentId, String paymentUrl) {
    return new PaymentResponse(
        paymentId,
        "transaction-id",
        paymentUrl,
        "PENDING",
        new BigDecimal("5000"),
        "MGA",
        "Unlock seller contact for offer offer-id",
        "PROFILE_UNLOCK",
        unlockId,
        buyerId,
        sellerId,
        "VANILLA_PAY");
  }
}
