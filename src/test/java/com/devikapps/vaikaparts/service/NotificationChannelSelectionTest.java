package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.mapper.NotificationMapper;
import com.devikapps.vaikaparts.mapper.user.*;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.model.user.*;
import com.devikapps.vaikaparts.repository.*;
import com.devikapps.vaikaparts.repository.model.user.*;
import com.devikapps.vaikaparts.service.notification.NotificationChannel;
import com.devikapps.vaikaparts.service.notification.NotificationMessageResolver;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import com.devikapps.vaikaparts.service.notification.delivery.ExternalNotificationDeliveryService;
import com.devikapps.vaikaparts.service.util.Paginator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class NotificationChannelSelectionTest {
  private final NotificationChannel inApp = mock(NotificationChannel.class);
  private final NotificationChannel email = mock(NotificationChannel.class);
  private final NotificationChannel sms = mock(NotificationChannel.class);
  private final NotificationChannel webPush = mock(NotificationChannel.class);
  private final ExternalNotificationDeliveryService deliveries =
      mock(ExternalNotificationDeliveryService.class);
  private final DemandPublishedNotificationRepository notifications =
      mock(DemandPublishedNotificationRepository.class);
  private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
  private final UserRepository users = mock(UserRepository.class);
  private final SellerMapper sellers = mock(SellerMapper.class);
  private final ResearcherMapper researchers = mock(ResearcherMapper.class);
  private final ManagerMapper managers = mock(ManagerMapper.class);
  private final DemandService demandService = mock(DemandService.class);
  private final OfferService offerService = mock(OfferService.class);
  private final NotificationService service =
      new NotificationService(
          List.of(email, sms, inApp, webPush),
          users,
          sellers,
          researchers,
          managers,
          demandService,
          offerService,
          notifications,
          mock(Paginator.class),
          mock(UserService.class),
          notificationMapper,
          new NotificationMessageResolver(),
          deliveries);

  static Stream<Arguments> webPushCases() {
    return Stream.of(
            Arguments.of(NotificationType.SYSTEM_ANNOUNCEMENT, UserType.SELLER),
            Arguments.of(NotificationType.SYSTEM_ANNOUNCEMENT, UserType.RESEARCHER),
            Arguments.of(NotificationType.SYSTEM_ANNOUNCEMENT, UserType.MANAGER),
            Arguments.of(NotificationType.DEMAND_CANCELED, UserType.SELLER),
            Arguments.of(NotificationType.DEMAND_CANCELED, UserType.RESEARCHER),
            Arguments.of(NotificationType.DEMAND_CANCELED, UserType.MANAGER),
            Arguments.of(NotificationType.OFFER_PUBLISHED, UserType.RESEARCHER),
            Arguments.of(NotificationType.DEMAND_PUBLISHED, UserType.SELLER),
            Arguments.of(NotificationType.DEMAND_PUBLISHED, UserType.MANAGER),
            Arguments.of(NotificationType.OFFER_ACCEPTED, UserType.SELLER),
            Arguments.of(NotificationType.OFFER_ACCEPTED, UserType.MANAGER),
            Arguments.of(NotificationType.OFFER_REJECTED, UserType.SELLER),
            Arguments.of(NotificationType.OFFER_REJECTED, UserType.MANAGER),
            Arguments.of(NotificationType.CONTACT_UNLOCKED, UserType.SELLER),
            Arguments.of(NotificationType.CONTACT_UNLOCKED, UserType.RESEARCHER))
        .flatMap(
            args ->
                Stream.of(false, true)
                    .flatMap(
                        emailPreference ->
                            Stream.of(false, true)
                                .map(
                                    smsPreference ->
                                        Arguments.of(
                                            args.get()[0],
                                            args.get()[1],
                                            emailPreference,
                                            smsPreference))));
  }

  @ParameterizedTest
  @MethodSource("webPushCases")
  void webPushIsSelectedForEveryValidTypeAndRoleRegardlessOfEmailSmsPreferences(
      NotificationType type, UserType role, boolean wantsEmail, boolean wantsSms) {
    var user = prepare(role, wantsEmail, wantsSms);
    user.setEmail(null);
    user.setPhoneNumber(null);
    when(webPush.isEnabled()).thenReturn(true);
    when(demandService.getDemandByIdWithoutAuthFilter("resource"))
        .thenReturn(Demand.builder().id("resource").build());
    when(offerService.getOfferByIdWithoutAuthFilter("resource"))
        .thenReturn(Offer.builder().id("resource").build());
    TransactionSynchronizationManager.initSynchronization();
    try {
      var notification =
          service.createAndSendNotification(
              NotificationRequest.builder()
                  .recipientUserId("user")
                  .notificationType(type)
                  .resourceId("resource")
                  .message("Test")
                  .build());
      verify(deliveries).enqueue(notification, List.of(webPush));
      verify(inApp).send(notification);
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      verify(webPush, never()).send(any());
      verify(email, never()).send(any());
      verify(sms, never()).send(any());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void webPushIsQueuedWhileEmailAndSmsKeepTheirAfterCommitTiming() {
    prepare(UserType.RESEARCHER, true, true);
    when(webPush.isEnabled()).thenReturn(true);
    TransactionSynchronizationManager.initSynchronization();
    try {
      var notification = service.createAndSendNotification(request());
      verify(deliveries).enqueue(notification, List.of(webPush));
      verify(email, never()).send(any());
      verify(sms, never()).send(any());
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      verify(email).send(notification);
      verify(sms).send(notification);
      verify(webPush, never()).send(any());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void sellerDemandKeepsAllSelectedExternalChannelsInTheQueue() {
    prepare(UserType.SELLER, true, true);
    when(webPush.isEnabled()).thenReturn(true);
    when(demandService.getDemandByIdWithoutAuthFilter("demand"))
        .thenReturn(Demand.builder().id("demand").build());
    var notification =
        service.createAndSendNotification(
            NotificationRequest.builder()
                .recipientUserId("user")
                .notificationType(NotificationType.DEMAND_PUBLISHED)
                .resourceId("demand")
                .message("Test")
                .build());
    verify(deliveries).enqueue(notification, List.of(email, sms, webPush));
    verify(email, never()).send(any());
    verify(sms, never()).send(any());
    verify(webPush, never()).send(any());
  }

  @Test
  void duplicateRequestDoesNotEnqueueAnotherWebPushDelivery() {
    prepare(UserType.SELLER, false, false);
    when(webPush.isEnabled()).thenReturn(true);
    var request = request();
    request.setNotificationRequestedId("request-1");
    var first = service.createAndSendNotification(request);
    var row =
        com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification.builder()
            .id(first.getId())
            .build();
    when(notifications.findByRequestId("request-1")).thenReturn(Optional.of(row));
    when(notificationMapper.toDomain(row)).thenReturn(first);
    assertSame(first, service.createAndSendNotification(request));
    verify(inApp).send(first);
    verify(deliveries).enqueue(first, List.of(webPush));
  }

  private User prepare(UserType type, boolean enableEmail, boolean enableSms) {
    when(webPush.getChannelType()).thenReturn(WEB_PUSH);
    when(inApp.getChannelType()).thenReturn(IN_APP);
    when(email.getChannelType()).thenReturn(EMAIL);
    when(sms.getChannelType()).thenReturn(SMS);
    when(inApp.isEnabled()).thenReturn(true);
    when(email.isEnabled()).thenReturn(true);
    when(sms.isEnabled()).thenReturn(true);
    JUser row;
    User user;
    switch (type) {
      case SELLER -> {
        var seller = Seller.builder().build();
        row = JSeller.builder().build();
        when(sellers.toSeller((JSeller) row)).thenReturn(seller);
        user = seller;
      }
      case RESEARCHER -> {
        var researcher = Researcher.builder().build();
        row = JResearcher.builder().build();
        when(researchers.toResearcher((JResearcher) row)).thenReturn(researcher);
        user = researcher;
      }
      case MANAGER -> {
        var manager = Manager.builder().build();
        row = JManager.builder().build();
        when(managers.toManager((JManager) row)).thenReturn(manager);
        user = manager;
      }
      default -> throw new AssertionError(type);
    }
    row.setUserType(type);
    user.setId("user");
    user.setUserType(type);
    user.setEmail("user@example.com");
    user.setPhoneNumber("0321234567");
    user.setEmailNotificationsEnabled(enableEmail);
    user.setSmsNotificationsEnabled(enableSms);
    when(users.findJUserById("user")).thenReturn(Optional.of(row));
    return user;
  }

  private NotificationRequest request() {
    return NotificationRequest.builder()
        .recipientUserId("user")
        .notificationType(NotificationType.SYSTEM_ANNOUNCEMENT)
        .message("Test")
        .build();
  }

  private NotificationRequest rejectedOfferRequest() {
    when(offerService.getOfferByIdWithoutAuthFilter("offer"))
        .thenReturn(Offer.builder().id("offer").build());
    return NotificationRequest.builder()
        .recipientUserId("user")
        .resourceId("offer")
        .notificationType(NotificationType.OFFER_REJECTED)
        .message("Votre offre n'a pas ete retenue")
        .build();
  }

  static Stream<Arguments> preferences() {
    return Stream.of(UserType.values())
        .flatMap(
            type ->
                Stream.of(
                    Arguments.of(type, false, false), Arguments.of(type, true, false),
                    Arguments.of(type, false, true), Arguments.of(type, true, true)));
  }

  static Stream<Arguments> rejectedOfferEmailCases() {
    return Stream.of(
        Arguments.of(true, true, "user@example.com", true),
        Arguments.of(false, true, "user@example.com", false),
        Arguments.of(true, false, "user@example.com", false),
        Arguments.of(true, true, " ", false));
  }

  @ParameterizedTest
  @MethodSource("preferences")
  void selects_channels_for_all_users_and_only_after_commit(
      UserType type, boolean wantsEmail, boolean wantsSms) {
    prepare(type, wantsEmail, wantsSms);
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.createAndSendNotification(request());
      verify(inApp).send(any());
      verify(email, never()).send(any());
      verify(sms, never()).send(any());
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      verify(email, times(wantsEmail ? 1 : 0)).send(any());
      verify(sms, times(wantsSms ? 1 : 0)).send(any());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void rollback_does_not_send_external_notifications() {
    prepare(UserType.SELLER, true, true);
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.createAndSendNotification(request());
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
      verify(email, never()).send(any());
      verify(sms, never()).send(any());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void rejected_offer_never_uses_sms_even_when_seller_opted_in() {
    prepare(UserType.SELLER, true, true);
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.createAndSendNotification(rejectedOfferRequest());
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      verify(inApp).send(any());
      verify(email).send(any());
      verify(sms, never()).send(any());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @ParameterizedTest
  @MethodSource("rejectedOfferEmailCases")
  void rejected_offer_email_respects_preference_channel_and_address(
      boolean wantsEmail, boolean emailChannelEnabled, String emailAddress, boolean expectedEmail) {
    var seller = prepare(UserType.SELLER, wantsEmail, true);
    seller.setEmail(emailAddress);
    when(email.isEnabled()).thenReturn(emailChannelEnabled);
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.createAndSendNotification(rejectedOfferRequest());
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);
      verify(inApp).send(any());
      verify(email, times(expectedEmail ? 1 : 0)).send(any());
      verify(sms, never()).send(any());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void failed_email_does_not_prevent_sms_after_commit() {
    prepare(UserType.RESEARCHER, true, true);
    doThrow(new RuntimeException("SMTP error")).when(email).send(any());
    TransactionSynchronizationManager.initSynchronization();
    try {
      service.createAndSendNotification(request());
      assertDoesNotThrow(
          () ->
              TransactionSynchronizationManager.getSynchronizations()
                  .forEach(TransactionSynchronization::afterCommit));
      var order = inOrder(inApp, email, sms);
      order.verify(inApp).send(any());
      order.verify(email).send(any());
      order.verify(sms).send(any());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void disabled_channels_override_user_opt_in() {
    prepare(UserType.MANAGER, true, true);
    when(email.isEnabled()).thenReturn(false);
    when(sms.isEnabled()).thenReturn(false);
    service.createAndSendNotification(request());
    verify(inApp).send(any());
    verify(email, never()).send(any());
    verify(sms, never()).send(any());
  }

  @Test
  void missing_contacts_skip_external_channels() {
    var user = prepare(UserType.SELLER, true, true);
    user.setEmail(" ");
    user.setPhoneNumber(null);
    service.createAndSendNotification(request());
    verify(inApp).send(any());
    verify(email, never()).send(any());
    verify(sms, never()).send(any());
  }

  @Test
  void in_app_failure_stops_external_sends() {
    prepare(UserType.SELLER, true, true);
    doThrow(new RuntimeException("DB unavailable")).when(inApp).send(any());
    assertThrows(IllegalStateException.class, () -> service.createAndSendNotification(request()));
    verify(email, never()).send(any());
    verify(sms, never()).send(any());
  }
}
