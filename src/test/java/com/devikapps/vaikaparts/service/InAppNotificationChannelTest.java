package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.IN_APP;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.exception.NotificationDeliveryException;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.model.user.Manager;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotificationRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.notification.InAppNotificationChannel;
import com.devikapps.vaikaparts.service.notification.NotificationWebSocketService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InAppNotificationChannelTest {

  private static final String TEST_NOTIFICATION_ID = "notification-123";
  private static final String TEST_NOTIFICATION_REQUESTED_ID = "notification-requested-001";
  private static final String TEST_SELLER_ID = "seller-456";
  private static final String TEST_DEMAND_ID = "demand-789";
  private static final String TEST_MESSAGE = "New demand available";

  @Mock private DemandPublishedNotificationRepository demandPublishedNotificationRepository;
  @Mock private NotificationRequestedRepository notificationRequestedRepository;
  @Mock private DemandRepository demandRepository;
  @Mock private UserRepository userRepository;
  @Mock private OfferRepository offerRepository;
  @Mock private NotificationWebSocketService webSocketService;

  @InjectMocks private InAppNotificationChannel inAppChannel;

  private Notification testNotification;
  private JDemandPublishedNotificationRequested jDemandPublishedNotificationRequested;
  private JSeller jSeller;
  private JDemand jDemand;
  private JDemandPublishedNotification testJDemandPublishedNotification;

  @BeforeEach
  void setUp() {
    jDemandPublishedNotificationRequested =
        JDemandPublishedNotificationRequested.builder().id(TEST_NOTIFICATION_REQUESTED_ID).build();

    jSeller = JSeller.builder().id(TEST_SELLER_ID).build();

    jDemand = JDemand.builder().id(TEST_DEMAND_ID).build();

    testJDemandPublishedNotification =
        JDemandPublishedNotification.builder()
            .id(TEST_NOTIFICATION_ID)
            .notificationRequested(jDemandPublishedNotificationRequested)
            .recipient(jSeller)
            .demand(jDemand)
            .message(TEST_MESSAGE)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .read(false)
            .createdAt(LocalDateTime.now())
            .build();

    testNotification = buildTestNotification();
  }

  @Test
  void should_save_notification_to_database() {
    mockRepositoryReferences();
    when(demandPublishedNotificationRepository.save(any(JDemandPublishedNotification.class)))
        .thenReturn(testJDemandPublishedNotification);

    inAppChannel.send(testNotification);

    verify(notificationRequestedRepository, times(1))
        .getReferenceById(TEST_NOTIFICATION_REQUESTED_ID);
    verify(userRepository, times(1)).getReferenceById(TEST_SELLER_ID);
    verify(demandRepository, times(1)).getReferenceById(TEST_DEMAND_ID);
    verify(demandPublishedNotificationRepository, times(1))
        .save(any(JDemandPublishedNotification.class));
  }

  @Test
  void should_send_via_websocket() {
    mockRepositoryReferences();
    when(demandPublishedNotificationRepository.save(any(JDemandPublishedNotification.class)))
        .thenReturn(testJDemandPublishedNotification);

    inAppChannel.send(testNotification);

    verify(webSocketService, times(1)).sendNotificationToUser(TEST_SELLER_ID, testNotification);
  }

  @Test
  void should_save_system_notification_for_manager_without_resource() {
    var managerId = "manager-123";
    var jManager = JManager.builder().id(managerId).userType(UserType.MANAGER).build();
    var notification =
        Notification.builder()
            .id(TEST_NOTIFICATION_ID)
            .recipient(Manager.builder().id(managerId).userType(UserType.MANAGER).build())
            .message("System maintenance")
            .notificationType(NotificationType.SYSTEM_ANNOUNCEMENT)
            .createdAt(LocalDateTime.now())
            .build();
    when(userRepository.getReferenceById(managerId)).thenReturn(jManager);

    inAppChannel.send(notification);

    var captor = ArgumentCaptor.forClass(JDemandPublishedNotification.class);
    verify(demandPublishedNotificationRepository).save(captor.capture());
    assertEquals(jManager, captor.getValue().getRecipient());
    assertNull(captor.getValue().getDemand());
    assertNull(captor.getValue().getOffer());
    verify(webSocketService).sendNotificationToUser(managerId, notification);
  }

  @Test
  void should_not_fail_if_websocket_throws_exception() {
    mockRepositoryReferences();
    when(demandPublishedNotificationRepository.save(any(JDemandPublishedNotification.class)))
        .thenReturn(testJDemandPublishedNotification);
    doThrow(new RuntimeException("WebSocket error"))
        .when(webSocketService)
        .sendNotificationToUser(anyString(), any(Notification.class));

    assertDoesNotThrow(() -> inAppChannel.send(testNotification));

    verify(demandPublishedNotificationRepository, times(1))
        .save(any(JDemandPublishedNotification.class));
  }

  @Test
  void should_throw_exception_if_database_save_fails() {
    mockRepositoryReferences();
    when(demandPublishedNotificationRepository.save(any(JDemandPublishedNotification.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThrows(NotificationDeliveryException.class, () -> inAppChannel.send(testNotification));

    verify(webSocketService, never()).sendNotificationToUser(anyString(), any());
  }

  @Test
  void should_return_correct_channel_type() {
    assertEquals(IN_APP, inAppChannel.getChannelType());
  }

  @Test
  void should_be_enabled_by_default() {
    assertTrue(inAppChannel.isEnabled());
  }

  private void mockRepositoryReferences() {
    when(notificationRequestedRepository.getReferenceById(TEST_NOTIFICATION_REQUESTED_ID))
        .thenReturn(jDemandPublishedNotificationRequested);
    when(userRepository.getReferenceById(TEST_SELLER_ID)).thenReturn(jSeller);
    when(demandRepository.getReferenceById(TEST_DEMAND_ID)).thenReturn(jDemand);
  }

  private Notification buildTestNotification() {
    return Notification.builder()
        .id(TEST_NOTIFICATION_ID)
        .notificationRequestedId(TEST_NOTIFICATION_REQUESTED_ID)
        .recipient(Seller.builder().id(TEST_SELLER_ID).build())
        .resource(Demand.builder().id(TEST_DEMAND_ID).build())
        .message(TEST_MESSAGE)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .read(false)
        .createdAt(LocalDateTime.now())
        .build();
  }
}
