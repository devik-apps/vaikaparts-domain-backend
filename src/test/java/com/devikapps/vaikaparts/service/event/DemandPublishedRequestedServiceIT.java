package com.devikapps.vaikaparts.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.DemandPublishedRequestedRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.PartRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.time.LocalDateTime;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class DemandPublishedRequestedServiceIT extends FacadeIT {

  private static final String TEST_DEMAND_ID = "demand-123";
  private static final String TEST_RESEARCHER_ID = randomUUID().toString();
  private static final String TEST_RESEARCHER_SUPABASE_ID = randomUUID().toString();
  private static final String TEST_PART_NAME = "Headlight";
  private static final String TEST_CAR_BRAND = "Toyota";
  private static final String TEST_CAR_MODEL = "Corolla";
  private static final int TEST_CAR_YEAR = 2015;

  @Autowired private DemandPublishedRequestedService service;
  @Autowired private DemandPublishedRequestedRepository demandPublishedRequestedRepository;
  @Autowired private NotificationRequestedRepository notificationRequestedRepository;
  @Autowired private DemandRepository demandRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PartRepository partRepository;
  @Autowired private ValueObjectMapper vom;

  private JDemand testDemand;
  private JSeller testSeller1;
  private JSeller testSeller2;

  @BeforeEach
  void setUp() {
    var researcher = createTestResearcher();
    testDemand = createTestDemand(researcher);
    testSeller1 = createTestSeller("seller-1");
    testSeller2 = createTestSeller("seller-2");
  }

  @AfterEach
  void tearDown() {
    notificationRequestedRepository.deleteAll();
    demandPublishedRequestedRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void should_create_event_log_with_success_status() throws InterruptedException {
    val event = buildTestEvent();

    service.accept(event);
    Thread.sleep(2000);

    val savedLog = demandPublishedRequestedRepository.findById(event.getId());
    assertTrue(savedLog.isPresent());
    assertEquals(ProcessStatus.SUCCESS, savedLog.get().getStatus());
    assertEquals(testDemand.getId(), savedLog.get().getDemand().getId());
    assertEquals(2, savedLog.get().getTotalSellersToNotify());
    assertEquals(2, savedLog.get().getNotificationsSentCount());
    assertEquals(0, savedLog.get().getAttemptNb());
    assertNull(savedLog.get().getErrorMessage());
    assertNotNull(savedLog.get().getCreatedAt());
    assertNotNull(savedLog.get().getUpdatedAt());
    assertNotNull(savedLog.get().getCompletedAt());
    assertTrue(savedLog.get().getCompletedAt().isAfter(savedLog.get().getCreatedAt()));
    assertTrue(
        savedLog.get().getUpdatedAt().isAfter(savedLog.get().getCreatedAt())
            || savedLog.get().getUpdatedAt().isEqual(savedLog.get().getCreatedAt()));
  }

  @Test
  void should_create_notification_request_logs_for_each_seller() throws InterruptedException {
    val event = buildTestEvent();

    service.accept(event);
    Thread.sleep(2000);

    val notificationLogs = notificationRequestedRepository.findAll();
    assertEquals(2, notificationLogs.size());

    val sellerIds = notificationLogs.stream().map(log -> log.getSeller().getId()).toList();
    assertTrue(sellerIds.contains(testSeller1.getId()));
    assertTrue(sellerIds.contains(testSeller2.getId()));

    notificationLogs.forEach(
        log -> {
          assertEquals(event.getId(), log.getDemandPublishedRequested().getId());
          assertEquals(testDemand.getId(), log.getDemand().getId());
          assertEquals(NotificationType.DEMAND_PUBLISHED, log.getNotificationType());
          assertEquals(ProcessStatus.SUCCESS, log.getStatus());
          assertNotNull(log.getCreatedAt());
          assertNotNull(log.getUpdatedAt());
          assertNotNull(log.getCompletedAt());
          assertNull(log.getErrorMessage());
        });
  }

  @Test
  void should_set_status_to_failed_when_demand_not_found() {
    val event =
        DemandPublishedRequested.builder()
            .id(randomUUID().toString())
            .demandId("non-existent-demand")
            .build();

    val thrown = assertThrows(RuntimeException.class, () -> service.accept(event));
    assertNotNull(thrown.getMessage());

    val savedLog = demandPublishedRequestedRepository.findById(event.getId());
    assertFalse(savedLog.isPresent());
  }

  @Test
  void should_update_existing_event_log_on_retry() throws InterruptedException {
    val event = buildTestEvent();
    event.setAttemptNb(1);

    val existingLog =
        JDemandPublishedRequested.builder()
            .id(event.getId())
            .demand(testDemand)
            .status(ProcessStatus.FAILED)
            .attemptNb(0)
            .totalSellersToNotify(0)
            .notificationsSentCount(0)
            .errorMessage("Previous attempt failed")
            .createdAt(LocalDateTime.now().minusMinutes(5))
            .updatedAt(LocalDateTime.now().minusMinutes(5))
            .build();
    demandPublishedRequestedRepository.save(existingLog);

    service.accept(event);
    Thread.sleep(2000);

    val updatedLog = demandPublishedRequestedRepository.findById(event.getId());
    assertTrue(updatedLog.isPresent());
    assertEquals(ProcessStatus.SUCCESS, updatedLog.get().getStatus());
    assertEquals(1, updatedLog.get().getAttemptNb());
    assertEquals(2, updatedLog.get().getTotalSellersToNotify());
    assertEquals(2, updatedLog.get().getNotificationsSentCount());
    assertNull(updatedLog.get().getErrorMessage());
    assertTrue(updatedLog.get().getUpdatedAt().isAfter(existingLog.getUpdatedAt()));
    assertNotNull(updatedLog.get().getCompletedAt());
    assertTrue(updatedLog.get().getCompletedAt().isAfter(existingLog.getUpdatedAt()));
  }

  @Test
  void should_not_create_notifications_for_disabled_sellers() throws InterruptedException {
    testSeller1.setStatus(UserStatus.DISABLED);
    userRepository.save(testSeller1);

    val event = buildTestEvent();

    service.accept(event);
    Thread.sleep(2000);

    val savedLog = demandPublishedRequestedRepository.findById(event.getId());
    assertTrue(savedLog.isPresent());
    assertEquals(ProcessStatus.SUCCESS, savedLog.get().getStatus());
    assertEquals(1, savedLog.get().getTotalSellersToNotify());
    assertEquals(1, savedLog.get().getNotificationsSentCount());
    assertNull(savedLog.get().getErrorMessage());

    val notificationLogs = notificationRequestedRepository.findAll();
    assertEquals(1, notificationLogs.size());
    assertEquals(testSeller2.getId(), notificationLogs.getFirst().getSeller().getId());
    assertEquals(testDemand.getId(), notificationLogs.getFirst().getDemand().getId());
    assertEquals(
        NotificationType.DEMAND_PUBLISHED, notificationLogs.getFirst().getNotificationType());
  }

  @Test
  void should_succeed_with_no_active_sellers() throws InterruptedException {
    testSeller1.setStatus(UserStatus.DISABLED);
    testSeller2.setStatus(UserStatus.DISABLED);
    userRepository.save(testSeller1);
    userRepository.save(testSeller2);

    val event = buildTestEvent();

    service.accept(event);
    Thread.sleep(500);

    val savedLog = demandPublishedRequestedRepository.findById(event.getId());
    assertTrue(savedLog.isPresent());
    assertEquals(ProcessStatus.SUCCESS, savedLog.get().getStatus());
    assertEquals(0, savedLog.get().getTotalSellersToNotify());
    assertEquals(0, savedLog.get().getNotificationsSentCount());
    assertNull(savedLog.get().getErrorMessage());
    assertNotNull(savedLog.get().getCompletedAt());

    val notificationLogs = notificationRequestedRepository.findAll();
    assertTrue(notificationLogs.isEmpty());
  }

  @Test
  void should_link_notification_requests_to_event_log() throws InterruptedException {
    val event = buildTestEvent();

    service.accept(event);
    Thread.sleep(2000);

    val savedLog = demandPublishedRequestedRepository.findById(event.getId());
    assertTrue(savedLog.isPresent());

    val notificationLogs = notificationRequestedRepository.findAll();
    notificationLogs.forEach(
        log -> assertEquals(savedLog.get().getId(), log.getDemandPublishedRequested().getId()));
  }

  private DemandPublishedRequested buildTestEvent() {
    return DemandPublishedRequested.builder()
        .id(randomUUID().toString())
        .demandId(testDemand.getId())
        .build();
  }

  private JDemand createTestDemand(JResearcher researcher) {
    val demand =
        JDemand.builder()
            .id(TEST_DEMAND_ID)
            .description("Looking for headlight")
            .attachedPhotoBucketKeys(new ArrayList<>())
            .researcher(researcher)
            .status(PostStatus.PUBLISHED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    val part =
        JPart.builder()
            .id("part-" + randomUUID())
            .partName(TEST_PART_NAME)
            .carBrand(TEST_CAR_BRAND)
            .carModel(TEST_CAR_MODEL)
            .carYear(TEST_CAR_YEAR)
            .partCategory(PartCategory.FOG_LIGHTS)
            .imageBuckets(new ArrayList<>())
            .build();
    part.setDemand(demand);

    var savedDemand = demandRepository.save(demand);
    partRepository.save(part);
    savedDemand.setPart(part);
    return demandRepository.save(savedDemand);
  }

  private JResearcher createTestResearcher() {
    return userRepository.save(
        JResearcher.builder()
            .id(TEST_RESEARCHER_ID)
            .supabaseUserId(TEST_RESEARCHER_SUPABASE_ID)
            .name("John Researcher")
            .email("researcher-" + randomUUID() + "@gmail.com")
            .phoneNumber("+1234567890")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JSeller createTestSeller(String id) {
    return userRepository.save(
        JSeller.builder()
            .id(id + "-" + randomUUID())
            .supabaseUserId("supabase-" + id + "-" + randomUUID())
            .name("Seller " + id)
            .email(id + "-" + randomUUID() + "@gmail.com")
            .phoneNumber("+1234567890")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .build());
  }
}
