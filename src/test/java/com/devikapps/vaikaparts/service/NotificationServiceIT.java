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
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.DemandPublishedRequestedRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotificationRequested;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
  @Autowired private DemandPublishedNotificationRepository demandPublishedNotificationRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private DemandRepository demandRepository;
  @Autowired private NotificationRequestedRepository notificationRequestedRepository;
  @Autowired private DemandPublishedRequestedRepository jDemandPublishedRequestedRepository;
  @Autowired private ValueObjectMapper vom;

  private JSeller testSeller;
  private JResearcher testResearcher;
  private JDemand testDemand;
  private JDemandPublishedNotificationRequested testNotificationRequested;

  @BeforeEach
  void setUp() {
    testResearcher = createTestResearcher();
    testSeller = createTestSeller();
    testDemand = createTestDemand();
    testNotificationRequested = createTestNotificationRequested(testSeller, testDemand);
  }

  @AfterEach
  void tearDown() {
    demandPublishedNotificationRepository.deleteAll();
    notificationRequestedRepository.deleteAll();
    jDemandPublishedRequestedRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  private void authenticateSeller() {
    val authentication =
        new UsernamePasswordAuthenticationToken(TEST_SELLER_SUPABASE_ID, null, new ArrayList<>());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @Test
  void should_fetch_all_notifications_for_current_seller() {
    authenticateSeller();
    try {
      val request1 =
          buildRequest(testSeller.getId(), testDemand.getId(), testNotificationRequested.getId());
      val notifRequested2 = createTestNotificationRequested(testSeller, testDemand);
      val request2 = buildRequest(testSeller.getId(), testDemand.getId(), notifRequested2.getId());
      notificationService.createAndSendNotification(request1);
      notificationService.createAndSendNotification(request2);

      val page = notificationService.fetchAllNotification(0, 10);

      assertEquals(2, page.getTotalElements());
      assertEquals(2, page.getContent().size());
      assertEquals(1, page.getTotalPages());
      assertFalse(page.getContent().isEmpty());
      page.getContent()
          .forEach(
              n -> {
                assertEquals(testSeller.getId(), n.getRecipient().getId());
                assertFalse(n.isRead());
                assertNotNull(n.getId());
                assertNotNull(n.getCreatedAt());
                assertEquals(NotificationType.DEMAND_PUBLISHED, n.getNotificationType());
              });
      assertTrue(
          page.getContent().get(0).getCreatedAt().isAfter(page.getContent().get(1).getCreatedAt())
              || page.getContent()
                  .get(0)
                  .getCreatedAt()
                  .isEqual(page.getContent().get(1).getCreatedAt()));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void should_not_fetch_notifications_of_another_seller() {
    authenticateSeller();
    try {
      val otherSeller = createAnotherSeller();
      val otherNotifRequested = createTestNotificationRequested(otherSeller, testDemand);
      notificationService.createAndSendNotification(
          buildRequest(otherSeller.getId(), testDemand.getId(), otherNotifRequested.getId()));
      notificationService.createAndSendNotification(
          buildRequest(testSeller.getId(), testDemand.getId(), testNotificationRequested.getId()));

      val page = notificationService.fetchAllNotification(0, 10);

      assertEquals(1, page.getTotalElements());
      assertEquals(testSeller.getId(), page.getContent().getFirst().getRecipient().getId());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void should_fetch_notifications_with_pagination() {
    authenticateSeller();
    try {
      for (int i = 0; i < 5; i++) {
        val nr = createTestNotificationRequested(testSeller, testDemand);
        notificationService.createAndSendNotification(
            buildRequest(testSeller.getId(), testDemand.getId(), nr.getId()));
      }

      val page0 = notificationService.fetchAllNotification(0, 2);
      val page1 = notificationService.fetchAllNotification(1, 2);
      val page2 = notificationService.fetchAllNotification(2, 2);

      assertEquals(5, page0.getTotalElements());
      assertEquals(3, page0.getTotalPages());
      assertEquals(2, page0.getContent().size());
      assertEquals(2, page1.getContent().size());
      assertEquals(1, page2.getContent().size());

      val page0Ids = page0.getContent().stream().map(Notification::getId).toList();
      val page1Ids = page1.getContent().stream().map(Notification::getId).toList();
      assertTrue(page0Ids.stream().noneMatch(page1Ids::contains));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void should_fetch_empty_page_when_no_notifications() {
    authenticateSeller();
    try {
      val page = notificationService.fetchAllNotification(0, 10);

      assertEquals(0, page.getTotalElements());
      assertTrue(page.getContent().isEmpty());
      assertEquals(0, page.getTotalPages());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void should_get_notification_by_id() {
    authenticateSeller();
    try {
      val created =
          notificationService.createAndSendNotification(
              buildRequest(
                  testSeller.getId(), testDemand.getId(), testNotificationRequested.getId()));

      val fetched = notificationService.getNotification(created.getId());

      assertEquals(created.getId(), fetched.getId());
      assertEquals(testNotificationRequested.getId(), fetched.getNotificationRequestedId());
      assertEquals(testSeller.getId(), fetched.getRecipient().getId());
      assertEquals(TEST_SELLER_NAME, fetched.getRecipient().getName());
      assertEquals(testDemand.getId(), fetched.getResource().getId());
      assertEquals(TEST_MESSAGE, fetched.getMessage());
      assertEquals(NotificationType.DEMAND_PUBLISHED, fetched.getNotificationType());
      assertFalse(fetched.isRead());
      assertNull(fetched.getReadAt());
      assertEquals(TEST_CLICK_ACTION, fetched.getClickAction());
      assertNotNull(fetched.getCreatedAt());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void should_throw_when_getting_notification_not_owned_by_current_seller() {
    authenticateSeller();
    try {
      val otherSeller = createAnotherSeller();
      val otherNr = createTestNotificationRequested(otherSeller, testDemand);
      val otherNotification =
          notificationService.createAndSendNotification(
              buildRequest(otherSeller.getId(), testDemand.getId(), otherNr.getId()));

      val notificationId = otherNotification.getId();
      val ex =
          assertThrows(
              ResourceNotFoundException.class,
              () -> notificationService.getNotification(notificationId));

      assertTrue(ex.getMessage().contains(notificationId));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void should_throw_when_getting_non_existent_notification() {
    authenticateSeller();
    try {
      val ex =
          assertThrows(
              ResourceNotFoundException.class,
              () -> notificationService.getNotification("non-existent-id"));

      assertTrue(ex.getMessage().contains("non-existent-id"));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void should_mark_notification_as_read() {
    authenticateSeller();
    try {
      val created =
          notificationService.createAndSendNotification(
              buildRequest(
                  testSeller.getId(), testDemand.getId(), testNotificationRequested.getId()));
      assertFalse(created.isRead());
      assertNull(created.getReadAt());

      val marked = notificationService.markAsRead(created.getId());

      assertTrue(marked.isRead());
      assertNotNull(marked.getReadAt());
      assertEquals(created.getId(), marked.getId());
      assertEquals(testSeller.getId(), marked.getRecipient().getId());
      assertEquals(testDemand.getId(), marked.getResource().getId());
      assertEquals(TEST_MESSAGE, marked.getMessage());

      val persisted =
          demandPublishedNotificationRepository.findByIdAndRecipientId(
              created.getId(), created.getRecipient().getId());
      assertTrue(persisted.isPresent());
      assertTrue(persisted.get().isRead());
      assertNotNull(persisted.get().getReadAt());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void should_throw_when_marking_as_read_notification_not_owned_by_current_seller() {
    authenticateSeller();
    try {
      val otherSeller = createAnotherSeller();
      val otherNr = createTestNotificationRequested(otherSeller, testDemand);
      val otherNotification =
          notificationService.createAndSendNotification(
              buildRequest(otherSeller.getId(), testDemand.getId(), otherNr.getId()));

      val notificationId = otherNotification.getId();
      val ex =
          assertThrows(
              ResourceNotFoundException.class,
              () -> notificationService.markAsRead(notificationId));

      assertTrue(ex.getMessage().contains(notificationId));

      val persisted = demandPublishedNotificationRepository.findById(notificationId);
      assertTrue(persisted.isPresent());
      assertFalse(persisted.get().isRead());
      assertNull(persisted.get().getReadAt());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void should_throw_when_marking_non_existent_notification_as_read() {
    authenticateSeller();
    try {
      val ex =
          assertThrows(
              ResourceNotFoundException.class,
              () -> notificationService.markAsRead("non-existent-id"));

      assertTrue(ex.getMessage().contains("non-existent-id"));
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void should_create_and_send_notification_successfully() {
    val request =
        buildRequest(testSeller.getId(), testDemand.getId(), testNotificationRequested.getId());

    val notification = notificationService.createAndSendNotification(request);

    assertNotNull(notification);
    assertNotNull(notification.getId());
    assertEquals(testNotificationRequested.getId(), notification.getNotificationRequestedId());
    assertNotNull(notification.getRecipient());
    assertEquals(testSeller.getId(), notification.getRecipient().getId());
    assertEquals(TEST_SELLER_NAME, notification.getRecipient().getName());
    assertEquals(TEST_SELLER_EMAIL, notification.getRecipient().getEmail());
    assertNotNull(notification.getResource());
    assertEquals(testDemand.getId(), notification.getResource().getId());
    assertEquals(TEST_DESCRIPTION, notification.getResource().getDescription());
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

    val savedNotification = demandPublishedNotificationRepository.findById(notification.getId());
    assertTrue(savedNotification.isPresent());
    assertEquals(notification.getId(), savedNotification.get().getId());
    assertEquals(
        testNotificationRequested.getId(),
        savedNotification.get().getNotificationRequested().getId());
    assertEquals(testSeller.getId(), savedNotification.get().getRecipient().getId());
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
    assertEquals(testSeller.getId(), notification1.getRecipient().getId());
    assertEquals(seller2.getId(), notification2.getRecipient().getId());
    assertEquals(testNotificationRequested.getId(), notification1.getNotificationRequestedId());
    assertEquals(notificationRequested2.getId(), notification2.getNotificationRequestedId());

    val allNotifications = demandPublishedNotificationRepository.findAll();
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

    assertTrue(exception.getMessage().contains("No user with id=non-existent-seller was found"));
  }

  @Test
  void should_throw_exception_when_demand_not_found() {
    val request =
        buildRequest(testSeller.getId(), "non-existent-demand", testNotificationRequested.getId());

    assertThrows(
        ResourceNotFoundException.class,
        () -> notificationService.createAndSendNotification(request));
  }

  @Test
  void should_create_notification_with_demand_notification_types() {
    for (NotificationType type :
        new NotificationType[] {
          NotificationType.DEMAND_PUBLISHED, NotificationType.DEMAND_CANCELED
        }) {
      val notifRequested = createTestNotificationRequested(testSeller, testDemand);
      val request =
          NotificationRequest.builder()
              .notificationRequestedId(notifRequested.getId())
              .recipientUserId(testSeller.getId())
              .resourceId(testDemand.getId())
              .message("Test message for " + type)
              .notificationType(type)
              .clickAction(TEST_CLICK_ACTION)
              .build();

      val notification = notificationService.createAndSendNotification(request);

      assertNotNull(notification);
      assertEquals(type, notification.getNotificationType());
    }

    val allNotifications = demandPublishedNotificationRepository.findAll();
    assertEquals(2, allNotifications.size());
  }

  @Test
  void should_create_notification_with_null_click_action() {
    val request =
        NotificationRequest.builder()
            .notificationRequestedId(testNotificationRequested.getId())
            .recipientUserId(testSeller.getId())
            .resourceId(testDemand.getId())
            .message(TEST_MESSAGE)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .clickAction(null)
            .build();

    val notification = notificationService.createAndSendNotification(request);

    assertNotNull(notification);
    assertNull(notification.getClickAction());

    val savedNotification = demandPublishedNotificationRepository.findById(notification.getId());
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

    val savedNotification = demandPublishedNotificationRepository.findById(notification.getId());
    assertTrue(savedNotification.isPresent());
    assertFalse(savedNotification.get().isRead());
    assertNull(savedNotification.get().getReadAt());
  }

  @Test
  void should_include_demand_details_in_notification() {
    val request =
        buildRequest(testSeller.getId(), testDemand.getId(), testNotificationRequested.getId());

    val notification = notificationService.createAndSendNotification(request);

    var demand = (Demand) notification.getResource();
    assertNotNull(demand);
    assertEquals(testDemand.getId(), demand.getId());
    assertEquals(TEST_DESCRIPTION, demand.getDescription());
    assertEquals(PostStatus.PUBLISHED, demand.getStatus());
    assertNotNull(demand.getPart());
    assertEquals(TEST_PART_NAME, demand.getPart().getName());
    assertEquals(TEST_CAR_BRAND, demand.getPart().getCarBrand());
    assertEquals(TEST_CAR_MODEL, demand.getPart().getCarModel());
    assertEquals(Year.of(TEST_CAR_YEAR), demand.getPart().getCarYear());
  }

  private NotificationRequest buildRequest(
      String sellerId, String demandId, String notificationRequestedId) {
    return NotificationRequest.builder()
        .notificationRequestedId(notificationRequestedId)
        .recipientUserId(sellerId)
        .resourceId(demandId)
        .message(TEST_MESSAGE)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .clickAction(TEST_CLICK_ACTION)
        .build();
  }

  private JDemandPublishedNotificationRequested createTestNotificationRequested(
      JSeller seller, JDemand demand) {
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
        JDemandPublishedNotificationRequested.builder()
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
