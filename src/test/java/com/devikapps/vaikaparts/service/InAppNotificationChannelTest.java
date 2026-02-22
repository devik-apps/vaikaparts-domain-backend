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
import com.devikapps.vaikaparts.mapper.NotificationMapper;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.NotificationRepository;
import com.devikapps.vaikaparts.repository.event.JNotification;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InAppNotificationChannelTest {

  private static final String TEST_NOTIFICATION_ID = "notification-123";
  private static final String TEST_SELLER_ID = "seller-456";
  private static final String TEST_DEMAND_ID = "demand-789";
  private static final String TEST_MESSAGE = "New demand available";

  @Mock private NotificationRepository notificationRepository;
  @Mock private NotificationMapper notificationMapper;
  @Mock private NotificationWebSocketService webSocketService;

  @InjectMocks private InAppNotificationChannel inAppChannel;

  private Notification testNotification;
  private JNotification testJNotification;

  @BeforeEach
  void setUp() {
    testNotification = buildTestNotification();
    testJNotification = buildTestJNotification();
  }

  @Test
  void should_save_notification_to_database() {
    when(notificationMapper.toPersistence(testNotification)).thenReturn(testJNotification);
    when(notificationRepository.save(testJNotification)).thenReturn(testJNotification);

    inAppChannel.send(testNotification);

    verify(notificationMapper, times(1)).toPersistence(testNotification);
    verify(notificationRepository, times(1)).save(testJNotification);
  }

  @Test
  void should_send_via_websocket() {
    when(notificationMapper.toPersistence(testNotification)).thenReturn(testJNotification);
    when(notificationRepository.save(testJNotification)).thenReturn(testJNotification);

    inAppChannel.send(testNotification);

    verify(webSocketService, times(1)).sendNotificationToSeller(TEST_SELLER_ID, testNotification);
  }

  @Test
  void should_not_fail_if_websocket_throws_exception() {
    when(notificationMapper.toPersistence(testNotification)).thenReturn(testJNotification);
    when(notificationRepository.save(testJNotification)).thenReturn(testJNotification);
    doThrow(new RuntimeException("WebSocket error"))
        .when(webSocketService)
        .sendNotificationToSeller(anyString(), any(Notification.class));

    assertDoesNotThrow(() -> inAppChannel.send(testNotification));

    verify(notificationRepository, times(1)).save(testJNotification);
  }

  @Test
  void should_throw_exception_if_database_save_fails() {
    when(notificationMapper.toPersistence(testNotification)).thenReturn(testJNotification);
    when(notificationRepository.save(testJNotification))
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

  private Notification buildTestNotification() {
    return Notification.builder()
        .id(TEST_NOTIFICATION_ID)
        .seller(Seller.builder().id(TEST_SELLER_ID).build())
        .demand(Demand.builder().id(TEST_DEMAND_ID).build())
        .message(TEST_MESSAGE)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .read(false)
        .createdAt(LocalDateTime.now())
        .build();
  }

  private JNotification buildTestJNotification() {
    return JNotification.builder()
        .id(TEST_NOTIFICATION_ID)
        .message(TEST_MESSAGE)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .read(false)
        .createdAt(LocalDateTime.now())
        .build();
  }
}
