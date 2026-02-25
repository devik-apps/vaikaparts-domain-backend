package com.devikapps.vaikaparts.service;

import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.exception.UserNotFoundException;
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
import com.devikapps.vaikaparts.repository.NotificationRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
import com.devikapps.vaikaparts.repository.event.JNotificationRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class NotificationServiceIT extends FacadeIT {

  private static final String TEST_SELLER_ID = "seller-123";
  private static final String TEST_SELLER_NAME = "John Seller";
  private static final String TEST_SELLER_EMAIL = "seller@test.com";
  private static final String TEST_SELLER_PHONE = "+1234567890";
  private static final String TEST_SELLER_SUPABASE_ID = "supabase-seller-123";

  private static final String TEST_RESEARCHER_ID = "researcher-456";
  private static final String TEST_RESEARCHER_NAME = "Jane Researcher";
  private static final String TEST_RESEARCHER_EMAIL = "researcher@test.com";
  private static final String TEST_RESEARCHER_SUPABASE_ID = "supabase-researcher-456";

  private static final String TEST_DEMAND_ID = "demand-789";
  private static final String TEST_DESCRIPTION = "Looking for Toyota Corolla headlight";
  private static final String TEST_PART_NAME = "Headlight";
  private static final String TEST_CAR_BRAND = "Toyota";
  private static final String TEST_CAR_MODEL = "Corolla";
  private static final int TEST_CAR_YEAR = 2015;
  private static final String TEST_MESSAGE = "New demand: Toyota Corolla Headlight (2015)";
  private static final String TEST_CLICK_ACTION =
      "{\"action\":\"VIEW_DEMAND\",\"demandId\":\"demand-789\"}";

  @Autowired private NotificationService notificationService;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private DemandRepository demandRepository;
  @Autowired private NotificationRequestedRepository notificationRequestedRepository;
  @Autowired private DemandPublishedRequestedRepository jDemandPublishedRequestedRepository;
  @Autowired private ValueObjectMapper vom;

  private JSeller testSeller;
  private JResearcher testResearcher;
  private JDemand testDemand;
  private JNotificationRequested testNotificationRequested;

  @BeforeEach
  void setUp() {
    testResearcher = createTestResearcher();
    testSeller = createTestSeller();
    testDemand = createTestDemand();
    testNotificationRequested = createTestNotificationRequested(testSeller, testDemand);
  }

  @AfterEach
  void tearDown() {
    notificationRepository.deleteAll();
    notificationRequestedRepository.deleteAll();
    jDemandPublishedRequestedRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void should_create_and_send_notification_successfully() {
    val request =
        buildRequest(testSeller.getId(), testDemand.getId(), testNotificationRequested.getId());

    val notification = notificationService.createAndSendNotification(request);

    assertNotNull(notification);
    assertNotNull(notification.getId());
    assertEquals(testNotificationRequested.getId(), notification.getNotificationRequestedId());
    assertNotNull(notification.getSeller());
    assertEquals(testSeller.getId(), notification.getSeller().getId());
    assertEquals(TEST_SELLER_NAME, notification.getSeller().getName());
    assertEquals(TEST_SELLER_EMAIL, notification.getSeller().getEmail());
    assertNotNull(notification.getDemand());
    assertEquals(testDemand.getId(), notification.getDemand().getId());
    assertEquals(TEST_DESCRIPTION, notification.getDemand().getDescription());
    assertEquals(TEST_MESSAGE, notification.getMessage());
    assertEquals(NotificationType.DEMAND_PUBLISHED, notification.getNotificationType());
    assertFalse(notification.isRead());
    assertEquals(TEST_CLICK_ACTION, notification.getClickAction());
    assertNotNull(notification.getCreatedAt());
    assertNull(notification.getReadAt());
  }

  @Test
  void should_save_notification_to_database() {
    val request =
        buildRequest(testSeller.getId(), testDemand.getId(), testNotificationRequested.getId());

    val notification = notificationService.createAndSendNotification(request);

    val savedNotification = notificationRepository.findById(notification.getId());
    assertTrue(savedNotification.isPresent());
    assertEquals(notification.getId(), savedNotification.get().getId());
    assertEquals(
        testNotificationRequested.getId(),
        savedNotification.get().getNotificationRequested().getId());
    assertEquals(testSeller.getId(), savedNotification.get().getSeller().getId());
    assertEquals(testDemand.getId(), savedNotification.get().getDemand().getId());
    assertEquals(TEST_MESSAGE, savedNotification.get().getMessage());
    assertEquals(NotificationType.DEMAND_PUBLISHED, savedNotification.get().getNotificationType());
    assertFalse(savedNotification.get().isRead());
    assertEquals(TEST_CLICK_ACTION, savedNotification.get().getClickAction());
    assertNotNull(savedNotification.get().getCreatedAt());
  }

  @Test
  void should_create_multiple_notifications_for_same_demand() {
    val seller2 = createAnotherSeller();
    val notificationRequested2 = createTestNotificationRequested(seller2, testDemand);

    val request1 =
        buildRequest(testSeller.getId(), testDemand.getId(), testNotificationRequested.getId());
    val request2 =
        buildRequest(seller2.getId(), testDemand.getId(), notificationRequested2.getId());

    val notification1 = notificationService.createAndSendNotification(request1);
    val notification2 = notificationService.createAndSendNotification(request2);

    assertNotNull(notification1);
    assertNotNull(notification2);
    assertNotEquals(notification1.getId(), notification2.getId());
    assertEquals(testSeller.getId(), notification1.getSeller().getId());
    assertEquals(seller2.getId(), notification2.getSeller().getId());
    assertEquals(testNotificationRequested.getId(), notification1.getNotificationRequestedId());
    assertEquals(notificationRequested2.getId(), notification2.getNotificationRequestedId());

    val allNotifications = notificationRepository.findAll();
    assertEquals(2, allNotifications.size());
  }

  @Test
  void should_throw_exception_when_seller_not_found() {
    val request =
        buildRequest("non-existent-seller", testDemand.getId(), testNotificationRequested.getId());

    UserNotFoundException exception =
        assertThrows(
            UserNotFoundException.class,
            () -> notificationService.createAndSendNotification(request));

    assertTrue(exception.getMessage().contains("No seller with id=non-existent-seller not found"));
  }

  @Test
  void should_throw_exception_when_demand_not_found() {
    val request =
        buildRequest(testSeller.getId(), "non-existent-demand", testNotificationRequested.getId());

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> notificationService.createAndSendNotification(request));
  }

  @Test
  void should_create_notification_with_all_notification_types() {
    for (NotificationType type : NotificationType.values()) {
      val notifRequested = createTestNotificationRequested(testSeller, testDemand);
      val request =
          NotificationRequest.builder()
              .notificationRequestedId(notifRequested.getId())
              .sellerId(testSeller.getId())
              .demandId(testDemand.getId())
              .message("Test message for " + type)
              .notificationType(type)
              .clickAction(TEST_CLICK_ACTION)
              .build();

      val notification = notificationService.createAndSendNotification(request);

      assertNotNull(notification);
      assertEquals(type, notification.getNotificationType());
    }

    val allNotifications = notificationRepository.findAll();
    assertEquals(NotificationType.values().length, allNotifications.size());
  }

  @Test
  void should_create_notification_with_null_click_action() {
    val request =
        NotificationRequest.builder()
            .notificationRequestedId(testNotificationRequested.getId())
            .sellerId(testSeller.getId())
            .demandId(testDemand.getId())
            .message(TEST_MESSAGE)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .clickAction(null)
            .build();

    val notification = notificationService.createAndSendNotification(request);

    assertNotNull(notification);
    assertNull(notification.getClickAction());

    val savedNotification = notificationRepository.findById(notification.getId());
    assertTrue(savedNotification.isPresent());
    assertNull(savedNotification.get().getClickAction());
  }

  @Test
  void should_set_notification_as_unread_by_default() {
    val request =
        buildRequest(testSeller.getId(), testDemand.getId(), testNotificationRequested.getId());

    val notification = notificationService.createAndSendNotification(request);

    assertFalse(notification.isRead());
    assertNull(notification.getReadAt());

    val savedNotification = notificationRepository.findById(notification.getId());
    assertTrue(savedNotification.isPresent());
    assertFalse(savedNotification.get().isRead());
    assertNull(savedNotification.get().getReadAt());
  }

  @Test
  void should_include_demand_details_in_notification() {
    val request =
        buildRequest(testSeller.getId(), testDemand.getId(), testNotificationRequested.getId());

    val notification = notificationService.createAndSendNotification(request);

    assertNotNull(notification.getDemand());
    assertEquals(testDemand.getId(), notification.getDemand().getId());
    assertEquals(TEST_DESCRIPTION, notification.getDemand().getDescription());
    assertEquals(PostStatus.PUBLISHED, notification.getDemand().getStatus());
    assertNotNull(notification.getDemand().getPart());
    assertEquals(TEST_PART_NAME, notification.getDemand().getPart().getName());
    assertEquals(TEST_CAR_BRAND, notification.getDemand().getPart().getCarBrand());
    assertEquals(TEST_CAR_MODEL, notification.getDemand().getPart().getCarModel());
    assertEquals(Year.of(TEST_CAR_YEAR), notification.getDemand().getPart().getCarYear());
  }

  private NotificationRequest buildRequest(
      String sellerId, String demandId, String notificationRequestedId) {
    return NotificationRequest.builder()
        .notificationRequestedId(notificationRequestedId)
        .sellerId(sellerId)
        .demandId(demandId)
        .message(TEST_MESSAGE)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .clickAction(TEST_CLICK_ACTION)
        .build();
  }

  private JNotificationRequested createTestNotificationRequested(JSeller seller, JDemand demand) {
    val jDemandPublishedRequested =
        jDemandPublishedRequestedRepository.save(
            JDemandPublishedRequested.builder()
                .id("dpr-" + currentTimeMillis())
                .demand(demand)
                .status(ProcessStatus.PENDING)
                .attemptNb(0)
                .totalSellersToNotify(1)
                .notificationsSentCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

    return notificationRequestedRepository.save(
        JNotificationRequested.builder()
            .id("nr-" + currentTimeMillis())
            .demandPublishedRequested(jDemandPublishedRequested)
            .seller(seller)
            .demand(demand)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .status(ProcessStatus.PENDING)
            .attemptNb(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build());
  }

  private JSeller createTestSeller() {
    return userRepository.save(
        JSeller.builder()
            .id(TEST_SELLER_ID)
            .supabaseUserId(TEST_SELLER_SUPABASE_ID)
            .name(TEST_SELLER_NAME)
            .email(TEST_SELLER_EMAIL)
            .phoneNumber(TEST_SELLER_PHONE)
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JSeller createAnotherSeller() {
    return userRepository.save(
        JSeller.builder()
            .id("seller-" + currentTimeMillis())
            .supabaseUserId("supabase-seller-" + currentTimeMillis())
            .name("Another Seller")
            .email("another-seller-" + currentTimeMillis() + "@test.com")
            .phoneNumber("+9876543210")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JResearcher createTestResearcher() {
    return userRepository.save(
        JResearcher.builder()
            .id(TEST_RESEARCHER_ID)
            .supabaseUserId(TEST_RESEARCHER_SUPABASE_ID)
            .name(TEST_RESEARCHER_NAME)
            .email(TEST_RESEARCHER_EMAIL)
            .phoneNumber("+1234567890")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JResearcher createAnotherResearcher() {
    return userRepository.save(
        JResearcher.builder()
            .id("researcher-" + currentTimeMillis())
            .supabaseUserId("supabase-researcher-" + currentTimeMillis())
            .name("Another Researcher")
            .email("another-researcher-" + currentTimeMillis() + "@test.com")
            .phoneNumber("+9876543210")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JDemand createTestDemand() {
    return createDemandForResearcher(testResearcher);
  }

  private JDemand createDemandForResearcher(JResearcher researcher) {
    val part =
        JPart.builder()
            .id("part-" + currentTimeMillis())
            .partName(TEST_PART_NAME)
            .carBrand(TEST_CAR_BRAND)
            .carModel(TEST_CAR_MODEL)
            .carYear(TEST_CAR_YEAR)
            .partCategory(PartCategory.FOG_LIGHTS)
            .imageBuckets(new ArrayList<>())
            .build();

    val demand =
        JDemand.builder()
            .id(researcher == testResearcher ? TEST_DEMAND_ID : "demand-" + currentTimeMillis())
            .description(TEST_DESCRIPTION)
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
}
