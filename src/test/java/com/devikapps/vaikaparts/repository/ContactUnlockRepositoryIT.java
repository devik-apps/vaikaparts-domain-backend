package com.devikapps.vaikaparts.repository;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.EXPIRED;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.FAILED;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;
import static com.devikapps.vaikaparts.model.classifier.PostStatus.PUBLISHED;
import static com.devikapps.vaikaparts.model.classifier.UserStatus.ENABLED;
import static com.devikapps.vaikaparts.model.classifier.UserType.RESEARCHER;
import static com.devikapps.vaikaparts.model.classifier.UserType.SELLER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.repository.model.exchange.JContactUnlock;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

class ContactUnlockRepositoryIT extends FacadeIT {

  @Autowired private ContactUnlockRepository contactUnlockRepository;
  @Autowired private OfferRepository offerRepository;
  @Autowired private DemandRepository demandRepository;
  @Autowired private UserRepository userRepository;

  private JResearcher buyer;
  private JSeller seller;
  private JOffer offer;

  @BeforeEach
  void setUp() {
    var now = OffsetDateTime.now();
    buyer =
        userRepository.save(
            JResearcher.builder()
                .id("buyer-id")
                .supabaseUserId("buyer-supabase-id")
                .name("Buyer")
                .email("buyer@example.com")
                .phoneNumber("+261340000001")
                .userType(RESEARCHER)
                .status(ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build());
    seller =
        userRepository.save(
            JSeller.builder()
                .id("seller-id")
                .supabaseUserId("seller-supabase-id")
                .name("Seller")
                .email("seller@example.com")
                .phoneNumber("+261340000002")
                .garageName("Garage")
                .userType(SELLER)
                .status(ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build());
    var demand =
        demandRepository.save(
            JDemand.builder()
                .id("demand-id")
                .researcher(buyer)
                .status(PUBLISHED)
                .createdAt(now.toLocalDateTime())
                .updatedAt(now.toLocalDateTime())
                .build());
    offer =
        offerRepository.save(
            JOffer.builder()
                .id("offer-id")
                .seller(seller)
                .demand(demand)
                .status(PUBLISHED)
                .createdAt(now.toLocalDateTime())
                .updatedAt(now.toLocalDateTime())
                .build());
  }

  @AfterEach
  void tearDown() {
    contactUnlockRepository.deleteAll();
    offerRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @Transactional
  void shouldPersistAllContactUnlockFieldsAndFindActiveAttempt() {
    var now = OffsetDateTime.now();
    var saved =
        contactUnlockRepository.saveAndFlush(
            unlock("unlock-id", "payment-id", "event-id", RELEASED, now));

    var reloaded =
        contactUnlockRepository.findByUnlockRequestIdForUpdate(saved.getUnlockRequestId()).orElseThrow();

    assertEquals("offer-id", reloaded.getOffer().getId());
    assertEquals("buyer-id", reloaded.getBuyer().getId());
    assertEquals("seller-id", reloaded.getSeller().getId());
    assertEquals("payment-id", reloaded.getPaymentId());
    assertEquals("event-id", reloaded.getReleaseEventId());
    assertEquals("VANILLA_PAY", reloaded.getProvider());
    assertEquals(RELEASED, reloaded.getStatus());
    assertEquals("https://pay.example/1", reloaded.getPaymentUrl());
    assertEquals(now.toInstant(), reloaded.getPaidAt().toInstant());
    assertEquals(now.toInstant(), reloaded.getReleasedAt().toInstant());
    assertTrue(
        contactUnlockRepository
            .findByBuyerIdAndOfferIdAndStatusIn("buyer-id", "offer-id", List.of(PENDING, RELEASED))
            .isPresent());
  }

  @Test
  void shouldRejectASecondActiveUnlockForTheSameBuyerAndOffer() {
    contactUnlockRepository.saveAndFlush(unlock("unlock-1", null, null, PENDING, null));

    assertThrows(
        DataIntegrityViolationException.class,
        () -> contactUnlockRepository.saveAndFlush(unlock("unlock-2", null, null, RELEASED, null)));
  }

  @Test
  void shouldAllowANewAttemptAfterFailedOrExpiredUnlock() {
    contactUnlockRepository.saveAndFlush(unlock("unlock-failed", null, null, FAILED, null));
    contactUnlockRepository.saveAndFlush(unlock("unlock-expired", null, null, EXPIRED, null));

    contactUnlockRepository.saveAndFlush(unlock("unlock-pending", null, null, PENDING, null));

    assertEquals(3, contactUnlockRepository.count());
  }

  @Test
  void shouldRejectDuplicatePaymentAndReleaseEventIdentifiers() {
    contactUnlockRepository.saveAndFlush(unlock("unlock-1", "payment-id", "event-id", FAILED, null));

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            contactUnlockRepository.saveAndFlush(
                unlock("unlock-2", "payment-id", "other-event-id", FAILED, null)));

    contactUnlockRepository.deleteAll();
    contactUnlockRepository.saveAndFlush(unlock("unlock-3", "payment-3", "event-id", FAILED, null));

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            contactUnlockRepository.saveAndFlush(
                unlock("unlock-4", "payment-4", "event-id", FAILED, null)));
  }

  private JContactUnlock unlock(
      String id,
      String paymentId,
      String releaseEventId,
      com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus status,
      OffsetDateTime completedAt) {
    var now = OffsetDateTime.now();
    return JContactUnlock.builder()
        .unlockRequestId(id)
        .offer(offer)
        .buyer(buyer)
        .seller(seller)
        .paymentId(paymentId)
        .releaseEventId(releaseEventId)
        .provider("VANILLA_PAY")
        .status(status)
        .paymentUrl("https://pay.example/1")
        .paidAt(completedAt)
        .releasedAt(completedAt)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
