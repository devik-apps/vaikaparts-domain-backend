package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.EMAIL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.mapper.NotificationMapper;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.notification.DemandPublishedNotification;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.notification.NotificationChannel;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import com.devikapps.vaikaparts.service.util.Paginator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  private static final String TEST_NOTIFICATION_REQUESTED_ID = "notification-requested-123";
  private static final String TEST_SELLER_ID = "seller-123";
  private static final String TEST_DEMAND_ID = "demand-456";
  private static final String TEST_MESSAGE = "New demand: Toyota Corolla Headlight";
  private static final String TEST_CLICK_ACTION =
      "{\"action\":\"VIEW_DEMAND\",\"demandId\":\"demand-456\"}";
  private static final String TEST_NOTIFICATION_ID = "notification-id-001";

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

    when(inAppChannel.isEnabled()).thenReturn(true);
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

    when(inAppChannel.isEnabled()).thenReturn(true);
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
    when(inAppChannel.isEnabled()).thenReturn(true);
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

    when(inAppChannel.isEnabled()).thenReturn(true);
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

    when(inAppChannel.isEnabled()).thenReturn(true);
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

  @Test
  void should_fetch_all_notifications_returns_mapped_page() {
    var seller = buildTestSeller();
    var pageRequest = PageRequest.of(0, 10);
    var jNotification = JDemandPublishedNotification.builder().build();
    var domainNotification = buildTestDomainNotification();
    var jPage = new PageImpl<>(List.of(jNotification), pageRequest, 1);

    when(paginator.apply(0, 10)).thenReturn(Map.of("page", 0, "size", 10));
    when(sellerService.getCurrentSeller()).thenReturn(seller);
    when(demandPublishedNotificationRepository.findBySellerIdOrderByCreatedAtDesc(
            eq(TEST_SELLER_ID), any(PageRequest.class)))
        .thenReturn(jPage);
    when(notificationMapper.toDomain(jNotification)).thenReturn(domainNotification);

    var result = notificationService.fetchAllNotification(0, 10);

    assertEquals(1, result.getTotalElements());
    assertEquals(1, result.getContent().size());
    assertEquals(TEST_SELLER_ID, result.getContent().getFirst().getSeller().getId());
    assertFalse(result.getContent().getFirst().isRead());
    verify(sellerService, times(1)).getCurrentSeller();
    verify(demandPublishedNotificationRepository, times(1))
        .findBySellerIdOrderByCreatedAtDesc(eq(TEST_SELLER_ID), any(PageRequest.class));
    verify(notificationMapper, times(1)).toDomain(jNotification);
  }

  @Test
  void should_fetch_all_notifications_returns_empty_page_when_none_exist() {
    var pageRequest = PageRequest.of(0, 10);
    Page<JDemandPublishedNotification> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

    when(paginator.apply(0, 10)).thenReturn(Map.of("page", 0, "size", 10));
    when(sellerService.getCurrentSeller()).thenReturn(buildTestSeller());
    when(demandPublishedNotificationRepository.findBySellerIdOrderByCreatedAtDesc(
            eq(TEST_SELLER_ID), any(PageRequest.class)))
        .thenReturn(emptyPage);

    var result = notificationService.fetchAllNotification(0, 10);

    assertEquals(0, result.getTotalElements());
    assertTrue(result.getContent().isEmpty());
    verify(notificationMapper, never()).toDomain(any());
  }

  @Test
  void should_get_notification_by_id_successfully() {
    var jNotification = JDemandPublishedNotification.builder().build();
    var domainNotification = buildTestDomainNotification();

    when(sellerService.getCurrentSeller()).thenReturn(buildTestSeller());
    when(demandPublishedNotificationRepository.findByIdAndSellerId(
            TEST_NOTIFICATION_ID, TEST_SELLER_ID))
        .thenReturn(Optional.of(jNotification));
    when(notificationMapper.toDomain(jNotification)).thenReturn(domainNotification);

    var result = notificationService.getNotification(TEST_NOTIFICATION_ID);

    assertEquals(TEST_NOTIFICATION_ID, result.getId());
    assertEquals(TEST_SELLER_ID, result.getSeller().getId());
    assertFalse(result.isRead());
    verify(sellerService, times(1)).getCurrentSeller();
    verify(demandPublishedNotificationRepository, times(1))
        .findByIdAndSellerId(TEST_NOTIFICATION_ID, TEST_SELLER_ID);
  }

  @Test
  void should_throw_when_getting_notification_not_found() {
    when(sellerService.getCurrentSeller()).thenReturn(buildTestSeller());
    when(demandPublishedNotificationRepository.findByIdAndSellerId(
            TEST_NOTIFICATION_ID, TEST_SELLER_ID))
        .thenReturn(Optional.empty());

    var ex =
        assertThrows(
            ResourceNotFoundException.class,
            () -> notificationService.getNotification(TEST_NOTIFICATION_ID));

    assertTrue(ex.getMessage().contains(TEST_NOTIFICATION_ID));
    verify(demandPublishedNotificationRepository, times(1))
        .findByIdAndSellerId(TEST_NOTIFICATION_ID, TEST_SELLER_ID);
    verify(notificationMapper, never()).toDomain(any());
  }

  @Test
  void should_mark_notification_as_read_successfully() {
    var jNotification = JDemandPublishedNotification.builder().read(false).build();
    var markedDomain = buildTestDomainNotification();
    markedDomain.setRead(true);

    when(sellerService.getCurrentSeller()).thenReturn(buildTestSeller());
    when(demandPublishedNotificationRepository.findByIdAndSellerId(
            TEST_NOTIFICATION_ID, TEST_SELLER_ID))
        .thenReturn(Optional.of(jNotification));
    when(notificationMapper.toDomain(jNotification)).thenReturn(markedDomain);

    var result = notificationService.markAsRead(TEST_NOTIFICATION_ID);

    assertTrue(result.isRead());
    assertTrue(jNotification.isRead());
    assertNotNull(jNotification.getReadAt());
    verify(demandPublishedNotificationRepository, times(1)).save(jNotification);
    verify(notificationMapper, times(1)).toDomain(jNotification);
  }

  @Test
  void should_throw_when_marking_as_read_notification_not_found() {
    when(sellerService.getCurrentSeller()).thenReturn(buildTestSeller());
    when(demandPublishedNotificationRepository.findByIdAndSellerId(
            TEST_NOTIFICATION_ID, TEST_SELLER_ID))
        .thenReturn(Optional.empty());

    var ex =
        assertThrows(
            ResourceNotFoundException.class,
            () -> notificationService.markAsRead(TEST_NOTIFICATION_ID));

    assertTrue(ex.getMessage().contains(TEST_NOTIFICATION_ID));
    verify(demandPublishedNotificationRepository, never()).save(any());
    verify(notificationMapper, never()).toDomain(any());
  }

  private DemandPublishedNotification buildTestDomainNotification() {
    return DemandPublishedNotification.builder()
        .id(TEST_NOTIFICATION_ID)
        .seller(buildTestSeller())
        .demand(buildTestDemand())
        .message(TEST_MESSAGE)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .read(false)
        .clickAction(TEST_CLICK_ACTION)
        .createdAt(LocalDateTime.now())
        .build();
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
