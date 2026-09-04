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
import com.devikapps.vaikaparts.mapper.user.ManagerMapper;
import com.devikapps.vaikaparts.mapper.user.ResearcherMapper;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.model.user.Manager;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
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
  @Mock private ResearcherMapper researcherMapper;
  @Mock private ManagerMapper managerMapper;
  @Mock private OfferService offerService;
  @Mock private Paginator paginator;
  @Mock private UserService userService;
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
            researcherMapper,
            managerMapper,
            demandService,
            offerService,
            demandPublishedNotificationRepository,
            paginator,
            userService,
            notificationMapper);
  }

  @Test
  void should_create_notification_with_correct_attributes() {
    var request = buildTestRequest();

    when(inAppChannel.isEnabled()).thenReturn(true);
    when(userRepository.findJUserById(TEST_SELLER_ID))
        .thenReturn(
            Optional.of(
                JSeller.builder()
                    .userType(com.devikapps.vaikaparts.model.classifier.UserType.SELLER)
                    .build()));
    when(sellerMapper.toSeller(any(JSeller.class))).thenReturn(buildTestSeller());
    when(demandService.getDemandByIdWithoutAuthFilter(TEST_DEMAND_ID))
        .thenReturn(buildTestDemand());

    var notification = notificationService.createAndSendNotification(request);

    assertNotNull(notification);
    assertNotNull(notification.getId());
    assertEquals(TEST_NOTIFICATION_REQUESTED_ID, notification.getNotificationRequestedId());
    assertEquals(TEST_SELLER_ID, notification.getRecipient().getId());
    assertEquals(TEST_DEMAND_ID, notification.getResource().getId());
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
        .thenReturn(
            Optional.of(
                JSeller.builder()
                    .userType(com.devikapps.vaikaparts.model.classifier.UserType.SELLER)
                    .build()));
    when(sellerMapper.toSeller(any(JSeller.class))).thenReturn(buildTestSeller());
    when(demandService.getDemandByIdWithoutAuthFilter(TEST_DEMAND_ID))
        .thenReturn(buildTestDemand());

    notificationService.createAndSendNotification(request);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(inAppChannel, times(1)).send(captor.capture());

    var sentNotification = captor.getValue();
    assertEquals(TEST_NOTIFICATION_REQUESTED_ID, sentNotification.getNotificationRequestedId());
    assertEquals(TEST_SELLER_ID, sentNotification.getRecipient().getId());
    assertEquals(TEST_DEMAND_ID, sentNotification.getResource().getId());
  }

  @Test
  void should_not_send_through_disabled_channels() {
    when(inAppChannel.isEnabled()).thenReturn(true);
    when(inAppChannel.isEnabled()).thenReturn(false);
    when(userRepository.findJUserById(TEST_SELLER_ID))
        .thenReturn(
            Optional.of(
                JSeller.builder()
                    .userType(com.devikapps.vaikaparts.model.classifier.UserType.SELLER)
                    .build()));
    when(sellerMapper.toSeller(any(JSeller.class))).thenReturn(buildTestSeller());
    when(demandService.getDemandByIdWithoutAuthFilter(TEST_DEMAND_ID))
        .thenReturn(buildTestDemand());

    notificationService.createAndSendNotification(buildTestRequest());

    verify(inAppChannel, never()).send(any(Notification.class));
  }

  @Test
  void should_continue_sending_to_other_channels_if_one_fails() {
    NotificationChannel failingChannel = mock(NotificationChannel.class);

    when(inAppChannel.isEnabled()).thenReturn(true);
    when(failingChannel.isEnabled()).thenReturn(true);
    when(failingChannel.getChannelType()).thenReturn(EMAIL);
    when(userRepository.findJUserById(TEST_SELLER_ID))
        .thenReturn(
            Optional.of(
                JSeller.builder()
                    .userType(com.devikapps.vaikaparts.model.classifier.UserType.SELLER)
                    .build()));
    when(sellerMapper.toSeller(any(JSeller.class))).thenReturn(buildTestSeller());
    when(demandService.getDemandByIdWithoutAuthFilter(TEST_DEMAND_ID))
        .thenReturn(buildTestDemand());
    doThrow(new RuntimeException("Channel failure")).when(failingChannel).send(any());

    notificationService =
        new NotificationService(
            List.of(failingChannel, inAppChannel),
            userRepository,
            sellerMapper,
            researcherMapper,
            managerMapper,
            demandService,
            offerService,
            demandPublishedNotificationRepository,
            paginator,
            userService,
            notificationMapper);

    assertDoesNotThrow(() -> notificationService.createAndSendNotification(buildTestRequest()));

    verify(failingChannel, times(1)).send(any(Notification.class));
    verify(inAppChannel, times(1)).send(any(Notification.class));
  }

  @Test
  void should_send_to_multiple_channels() {
    NotificationChannel emailChannel = mock(NotificationChannel.class);

    when(inAppChannel.isEnabled()).thenReturn(true);
    when(emailChannel.isEnabled()).thenReturn(true);
    when(emailChannel.getChannelType()).thenReturn(EMAIL);
    when(userRepository.findJUserById(TEST_SELLER_ID))
        .thenReturn(
            Optional.of(
                JSeller.builder()
                    .userType(com.devikapps.vaikaparts.model.classifier.UserType.SELLER)
                    .build()));
    when(sellerMapper.toSeller(any(JSeller.class))).thenReturn(buildTestSeller());
    when(demandService.getDemandByIdWithoutAuthFilter(TEST_DEMAND_ID))
        .thenReturn(buildTestDemand());

    notificationService =
        new NotificationService(
            List.of(inAppChannel, emailChannel),
            userRepository,
            sellerMapper,
            researcherMapper,
            managerMapper,
            demandService,
            offerService,
            demandPublishedNotificationRepository,
            paginator,
            userService,
            notificationMapper);

    notificationService.createAndSendNotification(buildTestRequest());

    verify(inAppChannel, times(1)).send(any(Notification.class));
    verify(emailChannel, times(1)).send(any(Notification.class));
  }

  @Test
  void should_build_system_announcement_for_manager_without_resource() {
    var managerId = "manager-123";
    var request =
        NotificationRequest.builder()
            .recipientUserId(managerId)
            .message("System maintenance")
            .notificationType(NotificationType.SYSTEM_ANNOUNCEMENT)
            .build();
    var manager = Manager.builder().id(managerId).userType(UserType.MANAGER).build();

    when(userRepository.findJUserById(managerId))
        .thenReturn(Optional.of(JManager.builder().userType(UserType.MANAGER).build()));
    when(managerMapper.toManager(any(JManager.class))).thenReturn(manager);

    var notification = notificationService.createAndSendNotification(request);

    assertEquals(managerId, notification.getRecipient().getId());
    assertEquals(UserType.MANAGER, notification.getRecipient().getUserType());
    assertNull(notification.getResource());
  }

  @Test
  void should_resolve_offer_for_offer_notification() {
    var offerId = "offer-123";
    var offer = Offer.builder().id(offerId).build();
    var request =
        NotificationRequest.builder()
            .recipientUserId(TEST_SELLER_ID)
            .resourceId(offerId)
            .notificationType(NotificationType.OFFER_ACCEPTED)
            .message(TEST_MESSAGE)
            .build();

    when(userRepository.findJUserById(TEST_SELLER_ID))
        .thenReturn(Optional.of(JSeller.builder().userType(UserType.SELLER).build()));
    when(sellerMapper.toSeller(any(JSeller.class))).thenReturn(buildTestSeller());
    when(offerService.getOfferByIdWithoutAuthFilter(offerId)).thenReturn(offer);

    var notification = notificationService.createAndSendNotification(request);

    assertEquals(offer, notification.getResource());
    verify(demandService, never()).getDemandByIdWithoutAuthFilter(any());
  }

  @Test
  void should_reject_demand_published_notification_for_researcher() {
    var researcherId = "researcher-123";
    var request =
        NotificationRequest.builder()
            .recipientUserId(researcherId)
            .resourceId(TEST_DEMAND_ID)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .message(TEST_MESSAGE)
            .build();

    when(userRepository.findJUserById(researcherId))
        .thenReturn(Optional.of(JResearcher.builder().userType(UserType.RESEARCHER).build()));
    when(researcherMapper.toResearcher(any(JResearcher.class)))
        .thenReturn(Researcher.builder().id(researcherId).userType(UserType.RESEARCHER).build());

    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> notificationService.createAndSendNotification(request));

    assertTrue(exception.getMessage().contains("DEMAND_PUBLISHED"));
    assertTrue(exception.getMessage().contains("RESEARCHER"));
  }

  @Test
  void should_fetch_all_notifications_returns_mapped_page() {
    var seller = buildTestSeller();
    var pageRequest = PageRequest.of(0, 10);
    var jNotification = JDemandPublishedNotification.builder().build();
    var domainNotification = buildTestDomainNotification();
    var jPage = new PageImpl<>(List.of(jNotification), pageRequest, 1);

    when(paginator.apply(0, 10)).thenReturn(Map.of("page", 0, "size", 10));
    when(userService.getCurrentUser())
        .thenReturn(JSeller.builder().id(TEST_SELLER_ID).userType(UserType.SELLER).build());
    when(demandPublishedNotificationRepository.findByRecipientIdOrderByCreatedAtDesc(
            eq(TEST_SELLER_ID), any(PageRequest.class)))
        .thenReturn(jPage);
    when(notificationMapper.toDomain(jNotification)).thenReturn(domainNotification);

    var result = notificationService.fetchAllNotification(0, 10);

    assertEquals(1, result.getTotalElements());
    assertEquals(1, result.getContent().size());
    assertEquals(TEST_SELLER_ID, result.getContent().getFirst().getRecipient().getId());
    assertFalse(result.getContent().getFirst().isRead());
    verify(userService, times(1)).getCurrentUser();
    verify(demandPublishedNotificationRepository, times(1))
        .findByRecipientIdOrderByCreatedAtDesc(eq(TEST_SELLER_ID), any(PageRequest.class));
    verify(notificationMapper, times(1)).toDomain(jNotification);
  }

  @Test
  void should_fetch_all_notifications_returns_empty_page_when_none_exist() {
    var pageRequest = PageRequest.of(0, 10);
    Page<JDemandPublishedNotification> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

    when(paginator.apply(0, 10)).thenReturn(Map.of("page", 0, "size", 10));
    when(userService.getCurrentUser())
        .thenReturn(JSeller.builder().id(TEST_SELLER_ID).userType(UserType.SELLER).build());
    when(demandPublishedNotificationRepository.findByRecipientIdOrderByCreatedAtDesc(
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

    when(userService.getCurrentUser())
        .thenReturn(JSeller.builder().id(TEST_SELLER_ID).userType(UserType.SELLER).build());
    when(demandPublishedNotificationRepository.findByIdAndRecipientId(
            TEST_NOTIFICATION_ID, TEST_SELLER_ID))
        .thenReturn(Optional.of(jNotification));
    when(notificationMapper.toDomain(jNotification)).thenReturn(domainNotification);

    var result = notificationService.getNotification(TEST_NOTIFICATION_ID);

    assertEquals(TEST_NOTIFICATION_ID, result.getId());
    assertEquals(TEST_SELLER_ID, result.getRecipient().getId());
    assertFalse(result.isRead());
    verify(userService, times(1)).getCurrentUser();
    verify(demandPublishedNotificationRepository, times(1))
        .findByIdAndRecipientId(TEST_NOTIFICATION_ID, TEST_SELLER_ID);
  }

  @Test
  void should_throw_when_getting_notification_not_found() {
    when(userService.getCurrentUser())
        .thenReturn(JSeller.builder().id(TEST_SELLER_ID).userType(UserType.SELLER).build());
    when(demandPublishedNotificationRepository.findByIdAndRecipientId(
            TEST_NOTIFICATION_ID, TEST_SELLER_ID))
        .thenReturn(Optional.empty());

    var ex =
        assertThrows(
            ResourceNotFoundException.class,
            () -> notificationService.getNotification(TEST_NOTIFICATION_ID));

    assertTrue(ex.getMessage().contains(TEST_NOTIFICATION_ID));
    verify(demandPublishedNotificationRepository, times(1))
        .findByIdAndRecipientId(TEST_NOTIFICATION_ID, TEST_SELLER_ID);
    verify(notificationMapper, never()).toDomain(any());
  }

  @Test
  void should_mark_notification_as_read_successfully() {
    var jNotification = JDemandPublishedNotification.builder().read(false).build();
    var markedDomain = buildTestDomainNotification();
    markedDomain.setRead(true);

    when(userService.getCurrentUser())
        .thenReturn(JSeller.builder().id(TEST_SELLER_ID).userType(UserType.SELLER).build());
    when(demandPublishedNotificationRepository.findByIdAndRecipientId(
            TEST_NOTIFICATION_ID, TEST_SELLER_ID))
        .thenReturn(Optional.of(jNotification));
    when(demandPublishedNotificationRepository.save(jNotification))
        .thenReturn(JDemandPublishedNotification.builder().read(true).build());
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
    when(userService.getCurrentUser())
        .thenReturn(JSeller.builder().id(TEST_SELLER_ID).userType(UserType.SELLER).build());
    when(demandPublishedNotificationRepository.findByIdAndRecipientId(
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

  private Notification buildTestDomainNotification() {
    return Notification.builder()
        .id(TEST_NOTIFICATION_ID)
        .recipient(buildTestSeller())
        .resource(buildTestDemand())
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
        .recipientUserId(TEST_SELLER_ID)
        .resourceId(TEST_DEMAND_ID)
        .message(TEST_MESSAGE)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .clickAction(TEST_CLICK_ACTION)
        .build();
  }

  private Seller buildTestSeller() {
    return Seller.builder()
        .id(TEST_SELLER_ID)
        .userType(com.devikapps.vaikaparts.model.classifier.UserType.SELLER)
        .build();
  }

  private Demand buildTestDemand() {
    return Demand.builder().id(TEST_DEMAND_ID).build();
  }
}
