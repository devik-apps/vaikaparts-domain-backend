package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;
import static com.devikapps.vaikaparts.model.classifier.PostStatus.PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.client.PecuniaClient;
import com.devikapps.vaikaparts.client.PecuniaClient.PapiPaymentResponse;
import com.devikapps.vaikaparts.client.PecuniaClient.PaymentResponse;
import com.devikapps.vaikaparts.client.PecuniaClientException;
import com.devikapps.vaikaparts.client.PecuniaNotFoundException;
import com.devikapps.vaikaparts.client.PecuniaUnknownOutcomeException;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.ContactUnlockRequest;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
  @Mock private ContactReleaseService contactReleaseService;

  private ContactUnlockService service;
  private JResearcher buyer;
  private JOffer offer;

  @BeforeEach
  void setUp() {
    buyer = JResearcher.builder().id("buyer-id").supabaseUserId("supabase-id").build();
    var seller = JSeller.builder().id("seller-id").build();
    var demand = JDemand.builder().id("demand-id").researcher(buyer).build();
    offer = JOffer.builder().id("offer-id").status(PUBLISHED).seller(seller).demand(demand).build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("supabase-id", null, java.util.List.of()));
    service =
        new ContactUnlockService(
            userRepository,
            offerRepository,
            persistence,
            pecuniaClient,
            contactReleaseService,
            new BigDecimal("5000"),
            "0320000000");
    lenient()
        .when(userRepository.findBySupabaseUserId("supabase-id"))
        .thenReturn(Optional.of(buyer));
    lenient()
        .when(offerRepository.findByIdWithRelations("offer-id"))
        .thenReturn(Optional.of(offer));
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
  void shouldReserveManualUnlockWithoutCallingPecunia() {
    when(persistence.findActive("buyer-id", "offer-id")).thenReturn(Optional.empty());
    when(persistence.reserve(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.initiate("offer-id", "MANUAL_ORANGE_MONEY");

    assertEquals(
        com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING_MANUAL_REVIEW,
        result.status());
    assertEquals("0320000000", result.paymentPhoneNumber());
    assertEquals(new BigDecimal("5000"), result.amount());
    assertEquals("MGA", result.currency());
    verify(pecuniaClient, never()).initiate(any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"MVOLA", "AIRTEL_MONEY", "ORANGE_MONEY"})
  void shouldInitiatePapiWithSavedResearcherIdentityAndNoHostedUrl(String method) {
    buyer.setName("Ada");
    buyer.setPhoneNumber("0340000000");
    when(persistence.findActive("buyer-id", "offer-id")).thenReturn(Optional.empty());
    when(persistence.reserve(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var temporaryCode = "ORANGE_MONEY".equals(method) ? "123456" : null;
    when(pecuniaClient.initiatePapi(any(), any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation -> papiPayment(invocation.getArgument(0), method, "Ada", "0340000000"));
    when(persistence.attachPapiPayment(any(), any())).thenAnswer(this::attachPapiPayment);

    var result =
        service.initiate("offer-id", new ContactUnlockRequest("PAPI", method, temporaryCode));

    assertEquals(PENDING, result.status());
    assertEquals(null, result.paymentUrl());
    var unlock = org.mockito.ArgumentCaptor.forClass(JContactUnlock.class);
    verify(persistence).reserve(unlock.capture());
    assertEquals("PAPI", unlock.getValue().getProvider());
    verify(pecuniaClient)
        .initiatePapi(
            unlock.getValue().getUnlockRequestId(),
            new BigDecimal("5000"),
            method,
            "Ada",
            "0340000000",
            temporaryCode);
  }

  @Test
  void shouldRejectPapiWhenSavedPayerDataOrOrangeCodeIsMissing() {
    assertThrows(
        IllegalStateException.class,
        () -> service.initiate("offer-id", new ContactUnlockRequest("PAPI", "MVOLA", null)));
    verify(pecuniaClient, never()).initiatePapi(any(), any(), any(), any(), any(), any());

    buyer.setName("Ada");
    buyer.setPhoneNumber("0340000000");
    assertThrows(
        IllegalArgumentException.class,
        () -> service.initiate("offer-id", new ContactUnlockRequest("PAPI", "ORANGE_MONEY", " ")));
    verify(persistence, never()).reserve(any());
    verify(pecuniaClient, never()).initiatePapi(any(), any(), any(), any(), any(), any());

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.initiate(
                "offer-id", new ContactUnlockRequest("PAPI", "MVOLA", "unexpected-code")));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.initiate("offer-id", new ContactUnlockRequest("PAPI", "UNKNOWN", null)));
  }

  @Test
  void shouldRejectInvalidPapiPayerBeforeReservingOrCallingPecunia() {
    buyer.setName("Ada");
    buyer.setPhoneNumber("not-a-madagascar-number");

    assertThrows(
        IllegalStateException.class,
        () -> service.initiate("offer-id", new ContactUnlockRequest("PAPI", "MVOLA", null)));
    buyer.setPhoneNumber("0340000000");
    buyer.setName("A".repeat(256));
    assertThrows(
        IllegalStateException.class,
        () -> service.initiate("offer-id", new ContactUnlockRequest("PAPI", "MVOLA", null)));

    verify(persistence, never()).reserve(any());
    verify(pecuniaClient, never()).initiatePapi(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldRejectPapiPriceBelowProviderMinimum() {
    service =
        new ContactUnlockService(
            userRepository,
            offerRepository,
            persistence,
            pecuniaClient,
            contactReleaseService,
            new BigDecimal("299"),
            "0320000000");
    buyer.setName("Ada");
    buyer.setPhoneNumber("0340000000");

    assertThrows(
        IllegalStateException.class,
        () -> service.initiate("offer-id", new ContactUnlockRequest("PAPI", "MVOLA", null)));
    verify(persistence, never()).reserve(any());
    verify(pecuniaClient, never()).initiatePapi(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldReleasePapiUnlockOnlyAfterPecuniaConfirmsSuccess() {
    var unlock = papiUnlock(PENDING);
    when(persistence.findOwned("buyer-id", "offer-id", "unlock-id"))
        .thenReturn(Optional.of(unlock));
    when(pecuniaClient.findPapiByReference("unlock-id"))
        .thenReturn(papiPayment("unlock-id", "MVOLA", "Ada", "0340000000", "SUCCESS"));
    when(persistence.attachPapiPayment(eq(unlock), any())).thenReturn(unlock);
    doAnswer(
            invocation -> {
              unlock.setStatus(RELEASED);
              return null;
            })
        .when(contactReleaseService)
        .releasePapi("unlock-id", "payment-id");

    var result = service.getUnlockStatus("offer-id", "unlock-id");
    var repeated = service.getUnlockStatus("offer-id", "unlock-id");

    assertEquals(RELEASED, result.status());
    assertEquals(RELEASED, repeated.status());
    verify(contactReleaseService).releasePapi("unlock-id", "payment-id");
    verify(pecuniaClient).findPapiByReference("unlock-id");
  }

  @Test
  void shouldMarkPapiUnlockFailedWhenPecuniaConfirmsFailure() {
    var unlock = papiUnlock(PENDING);
    when(persistence.findOwned("buyer-id", "offer-id", "unlock-id"))
        .thenReturn(Optional.of(unlock));
    when(pecuniaClient.findPapiByReference("unlock-id"))
        .thenReturn(papiPayment("unlock-id", "MVOLA", "Ada", "0340000000", "FAILED"));
    when(persistence.attachPapiPayment(eq(unlock), any())).thenReturn(unlock);
    when(persistence.failPapi(unlock, "payment-id"))
        .thenAnswer(
            invocation -> {
              unlock.setStatus(
                  com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.FAILED);
              return unlock;
            });

    var result = service.getUnlockStatus("offer-id", "unlock-id");

    assertEquals(
        com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.FAILED, result.status());
    verify(contactReleaseService, never()).releasePapi(any(), any());
  }

  @Test
  void shouldRejectPapiStatusWithMismatchedPaymentReference() {
    var unlock = papiUnlock(PENDING);
    when(persistence.findOwned("buyer-id", "offer-id", "unlock-id"))
        .thenReturn(Optional.of(unlock));
    when(pecuniaClient.findPapiByReference("unlock-id"))
        .thenReturn(papiPayment("different-reference", "MVOLA", "Ada", "0340000000", "SUCCESS"));

    assertThrows(
        IllegalStateException.class, () -> service.getUnlockStatus("offer-id", "unlock-id"));
    verify(contactReleaseService, never()).releasePapi(any(), any());
    verify(persistence, never()).failPapi(any(), any());
  }

  @Test
  void shouldRejectStatusWithMismatchedReservedPaymentMethod() {
    var unlock = papiUnlock(PENDING);
    unlock.setPaymentMethod("AIRTEL_MONEY");
    unlock.setPaymentAmount(new BigDecimal("5000"));
    when(persistence.findOwned("buyer-id", "offer-id", "unlock-id"))
        .thenReturn(Optional.of(unlock));
    when(pecuniaClient.findPapiByReference("unlock-id"))
        .thenReturn(papiPayment("unlock-id", "MVOLA", "Ada", "0340000000", "SUCCESS"));
    service =
        new ContactUnlockService(
            userRepository,
            offerRepository,
            persistence,
            pecuniaClient,
            contactReleaseService,
            new BigDecimal("6000"),
            "0320000000");

    assertThrows(
        IllegalStateException.class, () -> service.getUnlockStatus("offer-id", "unlock-id"));
    verify(contactReleaseService, never()).releasePapi(any(), any());
    verify(persistence, never()).failPapi(any(), any());
  }

  @Test
  void shouldValidateStatusAgainstReservedAmountAfterConfiguredPriceChanges() {
    var unlock = papiUnlock(PENDING);
    unlock.setPaymentAmount(new BigDecimal("5000"));
    when(persistence.findOwned("buyer-id", "offer-id", "unlock-id"))
        .thenReturn(Optional.of(unlock));
    when(pecuniaClient.findPapiByReference("unlock-id"))
        .thenReturn(papiPayment("unlock-id", "MVOLA", "Ada", "0340000000", "PENDING"));
    when(persistence.attachPapiPayment(eq(unlock), any())).thenReturn(unlock);
    service =
        new ContactUnlockService(
            userRepository,
            offerRepository,
            persistence,
            pecuniaClient,
            contactReleaseService,
            new BigDecimal("6000"),
            "0320000000");

    var result = service.getUnlockStatus("offer-id", "unlock-id");

    assertEquals(PENDING, result.status());
    assertEquals(new BigDecimal("5000"), result.amount());
  }

  @Test
  void shouldReturnNotFoundWhenAnotherResearcherOwnsTheUnlock() {
    when(persistence.findOwned("buyer-id", "offer-id", "unlock-id")).thenReturn(Optional.empty());

    assertThrows(
        com.devikapps.vaikaparts.exception.ResourceNotFoundException.class,
        () -> service.getUnlockStatus("offer-id", "unlock-id"));
    verify(pecuniaClient, never()).findPapiByReference(any());
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
  void shouldRetryUnknownPapiInitiationOnlyAfterReferenceLookupReturnsNotFound() {
    buyer.setName("Ada");
    buyer.setPhoneNumber("0340000000");
    when(persistence.findActive("buyer-id", "offer-id")).thenReturn(Optional.empty());
    when(persistence.reserve(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(pecuniaClient.initiatePapi(any(), any(), any(), any(), any(), any()))
        .thenThrow(new PecuniaUnknownOutcomeException("timeout"))
        .thenAnswer(
            invocation -> papiPayment(invocation.getArgument(0), "MVOLA", "Ada", "0340000000"));
    when(pecuniaClient.findPapiByReference(any()))
        .thenThrow(new PecuniaNotFoundException("not found"));
    when(persistence.attachPapiPayment(any(), any())).thenAnswer(this::attachPapiPayment);

    var result = service.initiate("offer-id", new ContactUnlockRequest("PAPI", "MVOLA", null));

    assertEquals(PENDING, result.status());
    var order = inOrder(persistence, pecuniaClient);
    order.verify(pecuniaClient).initiatePapi(any(), any(), any(), any(), any(), any());
    order.verify(pecuniaClient).findPapiByReference(result.unlockRequestId());
    order.verify(pecuniaClient).initiatePapi(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldReturnReservedUnlockWhenPapiOutcomeAndRecoveryLookupAreUnavailable() {
    buyer.setName("Ada");
    buyer.setPhoneNumber("0340000000");
    when(persistence.findActive("buyer-id", "offer-id")).thenReturn(Optional.empty());
    when(persistence.reserve(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(pecuniaClient.initiatePapi(any(), any(), any(), any(), any(), any()))
        .thenThrow(new PecuniaUnknownOutcomeException("timeout"));
    when(pecuniaClient.findPapiByReference(any()))
        .thenThrow(new PecuniaClientException("Pecunia unavailable"));

    var result = service.initiate("offer-id", new ContactUnlockRequest("PAPI", "MVOLA", null));

    assertEquals(PENDING, result.status());
    org.junit.jupiter.api.Assertions.assertNotNull(result.unlockRequestId());
    assertEquals(new BigDecimal("5000"), result.amount());
    verify(pecuniaClient).initiatePapi(any(), any(), any(), any(), any(), any());
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
    var response =
        payment("unlock-id", "buyer-id", "seller-id", "payment-id", "https://pay.example/1");
    var repository =
        org.mockito.Mockito.mock(com.devikapps.vaikaparts.repository.ContactUnlockRepository.class);
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

  private JContactUnlock attachPapiPayment(org.mockito.invocation.InvocationOnMock invocation) {
    JContactUnlock unlock = invocation.getArgument(0);
    PapiPaymentResponse payment = invocation.getArgument(1);
    unlock.setPaymentId(payment.id());
    return unlock;
  }

  private PapiPaymentResponse papiPayment(
      String reference, String method, String payerName, String payerPhone) {
    return papiPayment(reference, method, payerName, payerPhone, "PENDING");
  }

  private PapiPaymentResponse papiPayment(
      String reference, String method, String payerName, String payerPhone, String status) {
    return new PapiPaymentResponse(
        "payment-id",
        reference,
        reference,
        status,
        new BigDecimal("5000"),
        "MGA",
        "PROFILE_UNLOCK",
        "PAPI",
        method,
        new PecuniaClient.PapiParty(payerName, payerPhone),
        null);
  }

  private JContactUnlock papiUnlock(
      com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus status) {
    return JContactUnlock.builder()
        .unlockRequestId("unlock-id")
        .buyer(buyer)
        .seller(JSeller.builder().id("seller-id").build())
        .provider("PAPI")
        .paymentMethod("MVOLA")
        .paymentAmount(new BigDecimal("5000"))
        .status(status)
        .build();
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
