package com.devikapps.vaikaparts.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PartCondition;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.exchange.JPartInfo;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.notification.OfferOutcomeNotificationPublisher;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class DemandOutcomeConsistencyIT extends FacadeIT {

  private static final String RESEARCHER_SUPABASE_ID = "outcome-consistency-researcher";

  @Autowired private DemandService demandService;
  @Autowired private DemandRepository demandRepository;
  @Autowired private OfferRepository offerRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ValueObjectMapper valueObjectMapper;
  @MockitoBean private OfferOutcomeNotificationPublisher notificationPublisher;

  private JResearcher researcher;

  @BeforeEach
  void setUp() {
    researcher = createResearcher();
    authenticateResearcher();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    reset(notificationPublisher);
    offerRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void satisfyingDemandClosesAndNotifiesOnlyOtherPublishedOffers() {
    var demand = createDemand();
    var selected = createOffer(demand, PostStatus.PUBLISHED);
    var newlyClosed = createOffer(demand, PostStatus.PUBLISHED);
    var alreadyClosed = createOffer(demand, PostStatus.CLOSED);
    var canceled = createOffer(demand, PostStatus.CANCELED);

    demandService.updateOfferOutcome(demand.getId(), selected.getId(), PostStatus.SATISFIED);

    assertStatus(demand.getId(), PostStatus.SATISFIED);
    assertOfferStatus(selected.getId(), PostStatus.SATISFIED);
    assertOfferStatus(newlyClosed.getId(), PostStatus.CLOSED);
    assertOfferStatus(alreadyClosed.getId(), PostStatus.CLOSED);
    assertOfferStatus(canceled.getId(), PostStatus.CANCELED);
    verify(notificationPublisher)
        .publishRejectedOffers(
            argThat(
                offers ->
                    offers.size() == 1 && offers.getFirst().getId().equals(newlyClosed.getId())));

    assertThrows(
        IllegalStateException.class,
        () ->
            demandService.updateOfferOutcome(
                demand.getId(), selected.getId(), PostStatus.SATISFIED));
    verify(notificationPublisher, times(1))
        .publishRejectedOffers(argThat(offers -> offers.size() == 1));
  }

  @Test
  void explicitlyClosingOfferNotifiesOnlyThatNewlyClosedOffer() {
    var demand = createDemand();
    var closed = createOffer(demand, PostStatus.PUBLISHED);
    var untouched = createOffer(demand, PostStatus.PUBLISHED);

    demandService.updateOfferOutcome(demand.getId(), closed.getId(), PostStatus.CLOSED);

    assertOfferStatus(closed.getId(), PostStatus.CLOSED);
    assertOfferStatus(untouched.getId(), PostStatus.PUBLISHED);
    assertStatus(demand.getId(), PostStatus.PUBLISHED);
    verify(notificationPublisher)
        .publishRejectedOffers(
            argThat(
                offers -> offers.size() == 1 && offers.getFirst().getId().equals(closed.getId())));
  }

  @Test
  void satisfyingDemandWithNoOtherPublishedOfferDoesNotPublishRejection() {
    var demand = createDemand();
    var selected = createOffer(demand, PostStatus.PUBLISHED);

    demandService.updateOfferOutcome(demand.getId(), selected.getId(), PostStatus.SATISFIED);

    assertStatus(demand.getId(), PostStatus.SATISFIED);
    assertOfferStatus(selected.getId(), PostStatus.SATISFIED);
    verify(notificationPublisher, never()).publishRejectedOffers(argThat(offers -> true));
  }

  @Test
  void publisherFailureRollsBackEveryOutcomeChange() {
    var demand = createDemand();
    var selected = createOffer(demand, PostStatus.PUBLISHED);
    var rejected = createOffer(demand, PostStatus.PUBLISHED);
    doThrow(new IllegalStateException("publication failed"))
        .when(notificationPublisher)
        .publishRejectedOffers(argThat(offers -> true));

    assertThrows(
        IllegalStateException.class,
        () ->
            demandService.updateOfferOutcome(
                demand.getId(), selected.getId(), PostStatus.SATISFIED));

    assertStatus(demand.getId(), PostStatus.PUBLISHED);
    assertOfferStatus(selected.getId(), PostStatus.PUBLISHED);
    assertOfferStatus(rejected.getId(), PostStatus.PUBLISHED);
  }

  @Test
  void concurrentSelectionsAllowExactlyOneSelectedOffer() throws Exception {
    var demand = createDemand();
    var first = createOffer(demand, PostStatus.PUBLISHED);
    var second = createOffer(demand, PostStatus.PUBLISHED);
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);

    try (var executor = Executors.newFixedThreadPool(2)) {
      List<Future<Boolean>> outcomes =
          List.of(
              executor.submit(
                  () -> selectAfterBarrier(demand.getId(), first.getId(), ready, start)),
              executor.submit(
                  () -> selectAfterBarrier(demand.getId(), second.getId(), ready, start)));

      ready.await(10, TimeUnit.SECONDS);
      start.countDown();

      assertEquals(1, outcomes.stream().filter(this::completedSuccessfully).count());
    }

    assertStatus(demand.getId(), PostStatus.SATISFIED);
    assertEquals(
        1,
        List.of(first, second).stream()
            .map(offer -> offerRepository.findById(offer.getId()).orElseThrow().getStatus())
            .filter(status -> status == PostStatus.SATISFIED)
            .count());
    assertEquals(
        1,
        List.of(first, second).stream()
            .map(offer -> offerRepository.findById(offer.getId()).orElseThrow().getStatus())
            .filter(status -> status == PostStatus.CLOSED)
            .count());
    verify(notificationPublisher, times(1))
        .publishRejectedOffers(argThat(offers -> offers.size() == 1));
  }

  private boolean selectAfterBarrier(
      String demandId, String offerId, CountDownLatch ready, CountDownLatch start) {
    authenticateResearcher();
    ready.countDown();
    try {
      start.await(10, TimeUnit.SECONDS);
      demandService.updateOfferOutcome(demandId, offerId, PostStatus.SATISFIED);
      return true;
    } catch (IllegalStateException exception) {
      return false;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return false;
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private boolean completedSuccessfully(Future<Boolean> outcome) {
    try {
      return outcome.get(20, TimeUnit.SECONDS);
    } catch (Exception exception) {
      return false;
    }
  }

  private void authenticateResearcher() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(RESEARCHER_SUPABASE_ID, null, List.of()));
  }

  private void assertStatus(String demandId, PostStatus expected) {
    assertEquals(expected, demandRepository.findById(demandId).orElseThrow().getStatus());
  }

  private void assertOfferStatus(String offerId, PostStatus expected) {
    assertEquals(expected, offerRepository.findById(offerId).orElseThrow().getStatus());
  }

  private JResearcher createResearcher() {
    return userRepository.save(
        JResearcher.builder()
            .id("researcher-" + randomUUID())
            .supabaseUserId(RESEARCHER_SUPABASE_ID)
            .name("Researcher")
            .email(randomUUID() + "@example.com")
            .phoneNumber("+261340000001")
            .profileImgKey("")
            .location(valueObjectMapper.map(Location.getDefault()))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JSeller createSeller() {
    var suffix = randomUUID();
    return userRepository.save(
        JSeller.builder()
            .id("seller-" + suffix)
            .supabaseUserId("seller-supabase-" + suffix)
            .name("Seller")
            .email(suffix + "@example.com")
            .phoneNumber(
                "+261" + String.format("%09d", Math.floorMod(suffix.hashCode(), 1_000_000_000)))
            .profileImgKey("")
            .location(valueObjectMapper.map(Location.getDefault()))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JDemand createDemand() {
    var part =
        JPart.builder()
            .id("part-" + randomUUID())
            .partName("Headlight")
            .carBrand("Toyota")
            .carModel("Corolla")
            .carYear(2015)
            .partCategory(PartCategory.FOG_LIGHTS)
            .imageBuckets(new ArrayList<>())
            .build();
    var demand =
        JDemand.builder()
            .id("demand-" + randomUUID())
            .description("Need a headlight")
            .attachedPhotoBucketKeys(new ArrayList<>())
            .part(part)
            .researcher(researcher)
            .status(PostStatus.PUBLISHED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    part.setDemand(demand);
    return demandRepository.save(demand);
  }

  private JOffer createOffer(JDemand demand, PostStatus status) {
    var partInfo =
        JPartInfo.builder()
            .id("part-info-" + randomUUID())
            .partName("Headlight")
            .carBrand("Toyota")
            .carModel("Corolla")
            .carYear(2015)
            .partCategory(PartCategory.FOG_LIGHTS)
            .condition(PartCondition.USED)
            .price(150.0)
            .partImageBuckets(new ArrayList<>())
            .build();
    var offer =
        JOffer.builder()
            .id("offer-" + randomUUID())
            .demand(demand)
            .description("Available headlight")
            .attachedPhotoBucketKeys(new ArrayList<>())
            .partInfo(partInfo)
            .seller(createSeller())
            .status(status)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    partInfo.setOffer(offer);
    return offerRepository.save(offer);
  }
}
