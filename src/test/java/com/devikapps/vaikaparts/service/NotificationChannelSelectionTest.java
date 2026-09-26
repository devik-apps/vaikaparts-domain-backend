package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.mapper.NotificationMapper;
import com.devikapps.vaikaparts.mapper.user.*;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.user.*;
import com.devikapps.vaikaparts.repository.*;
import com.devikapps.vaikaparts.repository.model.user.*;
import com.devikapps.vaikaparts.service.notification.NotificationChannel;
import com.devikapps.vaikaparts.service.notification.NotificationMessageResolver;
import com.devikapps.vaikaparts.service.notification.NotificationService;
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
  private final UserRepository users = mock(UserRepository.class);
  private final SellerMapper sellers = mock(SellerMapper.class);
  private final ResearcherMapper researchers = mock(ResearcherMapper.class);
  private final ManagerMapper managers = mock(ManagerMapper.class);
  private final NotificationService service =
      new NotificationService(
          List.of(email, sms, inApp),
          users,
          sellers,
          researchers,
          managers,
          mock(DemandService.class),
          mock(OfferService.class),
          mock(DemandPublishedNotificationRepository.class),
          mock(Paginator.class),
          mock(UserService.class),
          mock(NotificationMapper.class),
          new NotificationMessageResolver());

  private User prepare(UserType type, boolean enableEmail, boolean enableSms) {
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

  static Stream<Arguments> preferences() {
    return Stream.of(UserType.values())
        .flatMap(
            type ->
                Stream.of(
                    Arguments.of(type, false, false), Arguments.of(type, true, false),
                    Arguments.of(type, false, true), Arguments.of(type, true, true)));
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
