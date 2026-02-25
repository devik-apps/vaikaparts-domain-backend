package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.IN_APP;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JNotification;
import com.devikapps.vaikaparts.repository.event.JNotificationRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

  @Mock private NotificationRepository notificationRepository;
  @Mock private NotificationRequestedRepository notificationRequestedRepository;
  @Mock private DemandRepository demandRepository;
  @Mock private UserRepository userRepository;
  @Mock private NotificationWebSocketService webSocketService;

  @InjectMocks private InAppNotificationChannel inAppChannel;

  private Notification testNotification;
  private JNotificationRequested jNotificationRequested;
  private JSeller jSeller;
  private JDemand jDemand;
  private JNotification testJNotification;

  @BeforeEach
  void setUp() {
    jNotificationRequested =
        JNotificationRequested.builder().id(TEST_NOTIFICATION_REQUESTED_ID).build();

    jSeller = JSeller.builder().id(TEST_SELLER_ID).build();

    jDemand = JDemand.builder().id(TEST_DEMAND_ID).build();

    testJNotification =
        JNotification.builder()
            .id(TEST_NOTIFICATION_ID)
            .notificationRequested(jNotificationRequested)
            .seller(jSeller)
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
    when(notificationRepository.save(any(JNotification.class))).thenReturn(testJNotification);

    inAppChannel.send(testNotification);

    verify(notificationRequestedRepository, times(1))
        .getReferenceById(TEST_NOTIFICATION_REQUESTED_ID);
    verify(userRepository, times(1)).findJUserById(TEST_SELLER_ID);
    verify(demandRepository, times(1)).getReferenceById(TEST_DEMAND_ID);
    verify(notificationRepository, times(1)).save(any(JNotification.class));
  }

  @Test
  void should_send_via_websocket() {
    mockRepositoryReferences();
    when(notificationRepository.save(any(JNotification.class))).thenReturn(testJNotification);

    inAppChannel.send(testNotification);

    verify(webSocketService, times(1)).sendNotificationToSeller(TEST_SELLER_ID, testNotification);
  }

  @Test
  void should_not_fail_if_websocket_throws_exception() {
    mockRepositoryReferences();
    when(notificationRepository.save(any(JNotification.class))).thenReturn(testJNotification);
    doThrow(new RuntimeException("WebSocket error"))
        .when(webSocketService)
        .sendNotificationToSeller(anyString(), any(Notification.class));

    assertDoesNotThrow(() -> inAppChannel.send(testNotification));

    verify(notificationRepository, times(1)).save(any(JNotification.class));
  }

  @Test
  void should_throw_exception_if_database_save_fails() {
    mockRepositoryReferences();
    when(notificationRepository.save(any(JNotification.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThrows(NotificationDeliveryException.class, () -> inAppChannel.send(testNotification));

    verify(webSocketService, never()).sendNotificationToSeller(anyString(), any());
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
        .thenReturn(jNotificationRequested);
    when(userRepository.findJUserById(TEST_SELLER_ID)).thenReturn(Optional.of(jSeller));
    when(demandRepository.getReferenceById(TEST_DEMAND_ID)).thenReturn(jDemand);
  }

  private Notification buildTestNotification() {
    return Notification.builder()
        .id(TEST_NOTIFICATION_ID)
        .notificationRequestedId(TEST_NOTIFICATION_REQUESTED_ID)
        .seller(Seller.builder().id(TEST_SELLER_ID).build())
        .demand(Demand.builder().id(TEST_DEMAND_ID).build())
        .message(TEST_MESSAGE)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .read(false)
        .createdAt(LocalDateTime.now())
        .build();
  }
}
