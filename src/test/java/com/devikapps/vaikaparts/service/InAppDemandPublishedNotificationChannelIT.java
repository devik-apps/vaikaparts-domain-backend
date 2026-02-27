package com.devikapps.vaikaparts.service;

import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.exception.NotificationDeliveryException;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.notification.DemandPublishedNotification;
import com.devikapps.vaikaparts.model.user.Seller;
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
import com.devikapps.vaikaparts.service.notification.InAppNotificationChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

class InAppDemandPublishedNotificationChannelIT extends FacadeIT {

  private static final String TEST_NOTIFICATION_ID = "notification-123";
  private static final String TEST_NOTIFICATION_REQUESTED_ID = "notification-requested-001";
  private static final String TEST_SELLER_ID = "seller-456";
  private static final String TEST_DEMAND_ID = "demand-789";
  private static final String TEST_MESSAGE = "New demand available";

  @Autowired private InAppNotificationChannel inAppNotificationChannel;
  @Autowired private DemandPublishedNotificationRepository demandPublishedNotificationRepository;
  @Autowired private NotificationRequestedRepository notificationRequestedRepository;
  @Autowired private DemandPublishedRequestedRepository demandPublishedRequestedRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private DemandRepository demandRepository;

  private DemandPublishedNotification testDemandPublishedNotification;

  @BeforeEach
  void setUp() {
    var jSeller =
        userRepository.save(
            JSeller.builder()
                .id(TEST_SELLER_ID)
                .supabaseUserId(randomUUID().toString())
                .name(randomUUID().toString())
                .userType(UserType.SELLER)
                .createdAt(now())
                .updatedAt(now())
                .status(UserStatus.ENABLED)
                .build());

    var jResearcher =
        userRepository.save(
            JResearcher.builder()
                .id(randomUUID().toString())
                .supabaseUserId(randomUUID().toString())
                .name(randomUUID().toString())
                .userType(UserType.RESEARCHER)
                .createdAt(now())
                .updatedAt(now())
                .status(UserStatus.ENABLED)
                .build());

    var jDemand =
        demandRepository.save(
            JDemand.builder()
                .id(TEST_DEMAND_ID)
                .researcher(jResearcher)
                .status(PostStatus.PUBLISHED)
                .build());

    var jPart =
        JPart.builder()
            .id(randomUUID().toString())
            .partName(randomUUID().toString())
            .partCategory(PartCategory.ENGINE_PART)
            .carBrand(randomUUID().toString())
            .carModel(randomUUID().toString())
            .carYear(2026)
            .demand(jDemand)
            .build();

    jDemand.setPart(jPart);
    demandRepository.save(jDemand);

    var jDemandPublishedRequested =
        demandPublishedRequestedRepository.save(
            JDemandPublishedRequested.builder()
                .id("demand-published-requested-001")
                .demand(jDemand)
                .status(ProcessStatus.PENDING)
                .attemptNb(0)
                .totalSellersToNotify(1)
                .notificationsSentCount(0)
                .createdAt(now())
                .updatedAt(now())
                .build());

    notificationRequestedRepository.save(
        JDemandPublishedNotificationRequested.builder()
            .id(TEST_NOTIFICATION_REQUESTED_ID)
            .demandPublishedRequested(jDemandPublishedRequested)
            .seller(jSeller)
            .demand(jDemand)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .status(ProcessStatus.PENDING)
            .attemptNb(0)
            .createdAt(now())
            .updatedAt(now())
            .build());

    testDemandPublishedNotification =
        DemandPublishedNotification.builder()
            .id(TEST_NOTIFICATION_ID)
            .notificationRequestedId(TEST_NOTIFICATION_REQUESTED_ID)
            .seller(Seller.builder().id(TEST_SELLER_ID).build())
            .demand(Demand.builder().id(TEST_DEMAND_ID).build())
            .message(TEST_MESSAGE)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .read(false)
            .createdAt(now())
            .build();
  }

  @AfterEach
  void tearDown() {
    demandPublishedNotificationRepository.deleteAll();
    notificationRequestedRepository.deleteAll();
    demandPublishedRequestedRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void should_save_notification_to_database() {
    inAppNotificationChannel.send(testDemandPublishedNotification);

    var saved = demandPublishedNotificationRepository.findById(TEST_NOTIFICATION_ID);
    assertTrue(saved.isPresent());
    assertEquals(TEST_NOTIFICATION_ID, saved.get().getId());
    assertEquals(TEST_MESSAGE, saved.get().getMessage());
    assertEquals(NotificationType.DEMAND_PUBLISHED, saved.get().getNotificationType());
    assertFalse(saved.get().isRead());
    assertNotNull(saved.get().getCreatedAt());
    assertEquals(TEST_NOTIFICATION_REQUESTED_ID, saved.get().getNotificationRequested().getId());
    assertEquals(TEST_SELLER_ID, saved.get().getSeller().getId());
    assertEquals(TEST_DEMAND_ID, saved.get().getDemand().getId());
  }

  @Test
  void should_save_notification_linked_to_correct_seller() {
    inAppNotificationChannel.send(testDemandPublishedNotification);

    var page =
        demandPublishedNotificationRepository.findBySellerIdOrderByCreatedAtDesc(
            TEST_SELLER_ID, Pageable.ofSize(10));
    assertEquals(1, page.getTotalElements());
    assertEquals(TEST_NOTIFICATION_ID, page.getContent().getFirst().getId());
  }

  @Test
  void should_not_fail_when_websocket_is_unavailable() {
    assertDoesNotThrow(() -> inAppNotificationChannel.send(testDemandPublishedNotification));
    assertEquals(1, demandPublishedNotificationRepository.count());
  }

  @Test
  void should_throw_notification_delivery_exception_when_notification_requested_does_not_exist() {
    var notificationWithBadRef =
        DemandPublishedNotification.builder()
            .id(TEST_NOTIFICATION_ID)
            .notificationRequestedId("non-existent-id")
            .seller(Seller.builder().id(TEST_SELLER_ID).build())
            .demand(Demand.builder().id(TEST_DEMAND_ID).build())
            .message(TEST_MESSAGE)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .read(false)
            .createdAt(now())
            .build();

    assertThrows(
        NotificationDeliveryException.class,
        () -> inAppNotificationChannel.send(notificationWithBadRef));
    assertEquals(0, demandPublishedNotificationRepository.count());
  }

  @Test
  void should_not_save_notification_when_notification_requested_does_not_exist() {
    var notificationWithBadRef =
        DemandPublishedNotification.builder()
            .id(TEST_NOTIFICATION_ID)
            .notificationRequestedId("non-existent-id")
            .seller(Seller.builder().id(TEST_SELLER_ID).build())
            .demand(Demand.builder().id(TEST_DEMAND_ID).build())
            .message(TEST_MESSAGE)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .read(false)
            .createdAt(now())
            .build();

    assertThrows(
        NotificationDeliveryException.class,
        () -> inAppNotificationChannel.send(notificationWithBadRef));
    assertEquals(0, demandPublishedNotificationRepository.count());
  }
}
