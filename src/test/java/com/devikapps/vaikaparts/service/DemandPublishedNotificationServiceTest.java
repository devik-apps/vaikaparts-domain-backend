package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.EMAIL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.mapper.NotificationMapper;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.notification.DemandPublishedNotification;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import com.devikapps.vaikaparts.service.util.Paginator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemandPublishedNotificationServiceTest {

  private static final String TEST_NOTIFICATION_REQUESTED_ID = "notification-requested-123";
  private static final String TEST_SELLER_ID = "seller-123";
  private static final String TEST_DEMAND_ID = "demand-456";
  private static final String TEST_MESSAGE = "New demand: Toyota Corolla Headlight";
  private static final String TEST_CLICK_ACTION =
      "{\"action\":\"VIEW_DEMAND\",\"demandId\":\"demand-456\"}";

  @Mock private NotificationChannel inAppChannel;
  @Mock private DemandService demandService;
  @Mock private UserRepository userRepository;
  @Mock private SellerMapper sellerMapper;
  @Mock private Paginator paginator;
  @Mock private SellerService sellerService;
  @Mock private NotificationMapper notificationMapper;
  @Mock private DemandPublishedNotificationRepository demandPublishedNotificationRepository;

  private NotificationService notificationService;

  @BeforeEach
  void setUp() {
    when(inAppChannel.isEnabled()).thenReturn(true);
    notificationService =
        new NotificationService(
            List.of(inAppChannel),
            userRepository,
            sellerMapper,
            demandService,
            demandPublishedNotificationRepository,
            paginator,
            sellerService,
            notificationMapper);
  }

  @Test
  void should_create_notification_with_correct_attributes() {
    var request = buildTestRequest();

    when(userRepository.findJUserById(TEST_SELLER_ID))
        .thenReturn(Optional.of(JSeller.builder().build()));
    when(sellerMapper.toSeller(any(JSeller.class))).thenReturn(buildTestSeller());
    when(demandService.getDemandByIdWithoutAuthFilter(TEST_DEMAND_ID))
        .thenReturn(buildTestDemand());

    var notification = notificationService.createAndSendNotification(request);

    assertNotNull(notification);
    assertNotNull(notification.getId());
    assertEquals(TEST_NOTIFICATION_REQUESTED_ID, notification.getNotificationRequestedId());
    assertEquals(TEST_SELLER_ID, notification.getSeller().getId());
    assertEquals(TEST_DEMAND_ID, notification.getDemand().getId());
    assertEquals(TEST_MESSAGE, notification.getMessage());
    assertEquals(NotificationType.DEMAND_PUBLISHED, notification.getNotificationType());
    assertEquals(TEST_CLICK_ACTION, notification.getClickAction());
    assertFalse(notification.isRead());
    assertNotNull(notification.getCreatedAt());
    assertNull(notification.getReadAt());
  }

  @Test
  void should_send_notification_through_enabled_channels() {
    var request = buildTestRequest();
    when(userRepository.findJUserById(TEST_SELLER_ID))
        .thenReturn(Optional.of(JSeller.builder().build()));
    when(sellerMapper.toSeller(any(JSeller.class))).thenReturn(buildTestSeller());
    when(demandService.getDemandByIdWithoutAuthFilter(TEST_DEMAND_ID))
        .thenReturn(buildTestDemand());

    notificationService.createAndSendNotification(request);

    ArgumentCaptor<DemandPublishedNotification> captor =
        ArgumentCaptor.forClass(DemandPublishedNotification.class);
    verify(inAppChannel, times(1)).send(captor.capture());

    var sentNotification = captor.getValue();
    assertEquals(TEST_NOTIFICATION_REQUESTED_ID, sentNotification.getNotificationRequestedId());
    assertEquals(TEST_SELLER_ID, sentNotification.getSeller().getId());
    assertEquals(TEST_DEMAND_ID, sentNotification.getDemand().getId());
  }

  @Test
  void should_not_send_through_disabled_channels() {
    when(inAppChannel.isEnabled()).thenReturn(false);
    when(userRepository.findJUserById(TEST_SELLER_ID))
        .thenReturn(Optional.of(JSeller.builder().build()));
    when(sellerMapper.toSeller(any(JSeller.class))).thenReturn(buildTestSeller());
    when(demandService.getDemandByIdWithoutAuthFilter(TEST_DEMAND_ID))
        .thenReturn(buildTestDemand());

    notificationService.createAndSendNotification(buildTestRequest());

    verify(inAppChannel, never()).send(any(DemandPublishedNotification.class));
  }

  @Test
  void should_continue_sending_to_other_channels_if_one_fails() {
    NotificationChannel failingChannel = mock(NotificationChannel.class);

    when(failingChannel.isEnabled()).thenReturn(true);
    when(failingChannel.getChannelType()).thenReturn(EMAIL);
    when(userRepository.findJUserById(TEST_SELLER_ID))
        .thenReturn(Optional.of(JSeller.builder().build()));
    when(sellerMapper.toSeller(any(JSeller.class))).thenReturn(buildTestSeller());
    when(demandService.getDemandByIdWithoutAuthFilter(TEST_DEMAND_ID))
        .thenReturn(buildTestDemand());
    doThrow(new RuntimeException("Channel failure")).when(failingChannel).send(any());

    notificationService =
        new NotificationService(
            List.of(failingChannel, inAppChannel),
            userRepository,
            sellerMapper,
            demandService,
            demandPublishedNotificationRepository,
            paginator,
            sellerService,
            notificationMapper);

    assertDoesNotThrow(() -> notificationService.createAndSendNotification(buildTestRequest()));

    verify(failingChannel, times(1)).send(any(DemandPublishedNotification.class));
    verify(inAppChannel, times(1)).send(any(DemandPublishedNotification.class));
  }

  @Test
  void should_send_to_multiple_channels() {
    NotificationChannel emailChannel = mock(NotificationChannel.class);
    when(emailChannel.isEnabled()).thenReturn(true);
    when(emailChannel.getChannelType()).thenReturn(EMAIL);
    when(userRepository.findJUserById(TEST_SELLER_ID))
        .thenReturn(Optional.of(JSeller.builder().build()));
    when(sellerMapper.toSeller(any(JSeller.class))).thenReturn(buildTestSeller());
    when(demandService.getDemandByIdWithoutAuthFilter(TEST_DEMAND_ID))
        .thenReturn(buildTestDemand());

    notificationService =
        new NotificationService(
            List.of(inAppChannel, emailChannel),
            userRepository,
            sellerMapper,
            demandService,
            demandPublishedNotificationRepository,
            paginator,
            sellerService,
            notificationMapper);

    notificationService.createAndSendNotification(buildTestRequest());

    verify(inAppChannel, times(1)).send(any(DemandPublishedNotification.class));
    verify(emailChannel, times(1)).send(any(DemandPublishedNotification.class));
  }

  private NotificationRequest buildTestRequest() {
    return NotificationRequest.builder()
        .notificationRequestedId(TEST_NOTIFICATION_REQUESTED_ID)
        .sellerId(TEST_SELLER_ID)
        .demandId(TEST_DEMAND_ID)
        .message(TEST_MESSAGE)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .clickAction(TEST_CLICK_ACTION)
        .build();
  }

  private Seller buildTestSeller() {
    return Seller.builder().id(TEST_SELLER_ID).build();
  }

  private Demand buildTestDemand() {
    return Demand.builder().id(TEST_DEMAND_ID).build();
  }
}
