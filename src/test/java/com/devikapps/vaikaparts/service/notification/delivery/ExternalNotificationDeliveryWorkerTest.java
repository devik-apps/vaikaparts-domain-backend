package com.devikapps.vaikaparts.service.notification.delivery;

import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.EMAIL;
import static com.devikapps.vaikaparts.model.classifier.NotificationChannelType.WEB_PUSH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.mapper.NotificationMapper;
import com.devikapps.vaikaparts.mapper.user.ManagerMapper;
import com.devikapps.vaikaparts.mapper.user.ResearcherMapper;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.ExternalNotificationDeliveryRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.WebPushSubscriptionRepository;
import com.devikapps.vaikaparts.repository.event.JExternalNotificationDelivery;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.DemandService;
import com.devikapps.vaikaparts.service.OfferService;
import com.devikapps.vaikaparts.service.UserService;
import com.devikapps.vaikaparts.service.notification.InAppNotificationChannel;
import com.devikapps.vaikaparts.service.notification.NotificationChannel;
import com.devikapps.vaikaparts.service.notification.NotificationMessageResolver;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import com.devikapps.vaikaparts.service.notification.NotificationWebSocketService;
import com.devikapps.vaikaparts.service.notification.WebPushNotificationChannel;
import com.devikapps.vaikaparts.service.notification.WebPushProperties;
import com.devikapps.vaikaparts.service.notification.push.WebPushEndpointValidator;
import com.devikapps.vaikaparts.service.notification.push.WebPushSender;
import com.devikapps.vaikaparts.service.util.Paginator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class ExternalNotificationDeliveryWorkerTest {

  @Test
  void marks_delivery_success_only_after_channel_returns() {
    var email = mock(NotificationChannel.class);
    var repository = mock(ExternalNotificationDeliveryRepository.class);
    when(email.getChannelType()).thenReturn(EMAIL);
    when(repository.markSuccess(anyString(), anyString(), any())).thenReturn(1);
    var worker =
        new ExternalNotificationDeliveryWorker(
            repository,
            List.of(email),
            properties(),
            mock(TransactionTemplate.class),
            mock(NotificationMapper.class));
    var delivery = pendingDelivery();
    var notification = Notification.builder().id("notification-1").build();

    worker.deliverClaimed(delivery, () -> notification);

    verify(email).send(notification);
    verify(repository).markSuccess(eq(delivery.getId()), eq("claim"), any());
  }

  @Test
  void reschedules_failed_delivery_with_backoff() {
    var email = mock(NotificationChannel.class);
    var repository = mock(ExternalNotificationDeliveryRepository.class);
    when(email.getChannelType()).thenReturn(EMAIL);
    var notification = Notification.builder().id("notification-1").build();
    doThrow(new IllegalStateException("smtp unavailable")).when(email).send(notification);
    when(repository.reschedule(anyString(), anyString(), anyInt(), any(), any(), any()))
        .thenReturn(1);
    var worker =
        new ExternalNotificationDeliveryWorker(
            repository,
            List.of(email),
            properties(),
            mock(TransactionTemplate.class),
            mock(NotificationMapper.class));
    var delivery = pendingDelivery();
    var dueAt = delivery.getNextAttemptAt();

    worker.deliverClaimed(delivery, () -> notification);

    var nextAttempt = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(repository)
        .reschedule(
            eq(delivery.getId()),
            eq("claim"),
            eq(1),
            nextAttempt.capture(),
            eq("smtp unavailable"),
            any());
    assertTrue(nextAttempt.getValue().isAfter(dueAt));
  }

  @Test
  void reschedules_delivery_when_notification_mapping_fails() {
    var email = mock(NotificationChannel.class);
    var repository = mock(ExternalNotificationDeliveryRepository.class);
    when(email.getChannelType()).thenReturn(EMAIL);
    when(repository.reschedule(anyString(), anyString(), anyInt(), any(), any(), any()))
        .thenReturn(1);
    var worker =
        new ExternalNotificationDeliveryWorker(
            repository,
            List.of(email),
            properties(),
            mock(TransactionTemplate.class),
            mock(NotificationMapper.class));
    var delivery = pendingDelivery();

    worker.deliverClaimed(
        delivery,
        () -> {
          throw new IllegalStateException("mapping failed");
        });

    verify(repository)
        .reschedule(eq(delivery.getId()), eq("claim"), eq(1), any(), eq("mapping failed"), any());
    verify(email, never()).send(any());
  }

  @Test
  void webPushFailureIsRetriedAfterNotificationCommit() {
    var push = mock(NotificationChannel.class);
    when(push.getChannelType()).thenReturn(WEB_PUSH);
    when(push.isEnabled()).thenReturn(true);
    doThrow(new IllegalStateException("push unavailable")).doNothing().when(push).send(any());
    var notifications = mock(DemandPublishedNotificationRepository.class);
    var users = mock(UserRepository.class);
    var sellers = mock(SellerMapper.class);
    var row = JSeller.builder().build();
    row.setUserType(UserType.SELLER);
    var seller = Seller.builder().id("user").userType(UserType.SELLER).build();
    when(users.findJUserById("user")).thenReturn(Optional.of(row));
    when(sellers.toSeller(row)).thenReturn(seller);
    var socket = mock(NotificationWebSocketService.class);
    var inApp =
        new InAppNotificationChannel(
            notifications,
            users,
            mock(NotificationRequestedRepository.class),
            socket,
            mock(DemandRepository.class),
            mock(OfferRepository.class));
    var repository = mock(ExternalNotificationDeliveryRepository.class);
    var queue = new ExternalNotificationDeliveryService(repository, notifications);
    var service =
        new NotificationService(
            List.of(inApp, push),
            users,
            sellers,
            mock(ResearcherMapper.class),
            mock(ManagerMapper.class),
            mock(DemandService.class),
            mock(OfferService.class),
            notifications,
            mock(Paginator.class),
            mock(UserService.class),
            mock(NotificationMapper.class),
            new NotificationMessageResolver(),
            queue);
    var committed = new AtomicBoolean();
    var transaction =
        new TransactionTemplate(
            new AbstractPlatformTransactionManager() {
              @Override
              protected Object doGetTransaction() {
                return new Object();
              }

              @Override
              protected void doBegin(Object value, TransactionDefinition definition) {}

              @Override
              protected void doCommit(DefaultTransactionStatus status) {
                committed.set(true);
              }

              @Override
              protected void doRollback(DefaultTransactionStatus status) {
                fail("Notification must not roll back");
              }
            });
    var notification =
        transaction.execute(
            status -> {
              var created =
                  service.createAndSendNotification(
                      NotificationRequest.builder()
                          .recipientUserId("user")
                          .notificationType(NotificationType.SYSTEM_ANNOUNCEMENT)
                          .message("Test")
                          .build());
              verify(notifications).save(any());
              verify(socket, never()).sendNotificationToUser(anyString(), any());
              verify(push, never()).send(any());
              return created;
            });
    assertTrue(committed.get());
    verify(socket).sendNotificationToUser("user", notification);
    var captor = ArgumentCaptor.forClass(JExternalNotificationDelivery.class);
    verify(repository).save(captor.capture());
    var delivery = captor.getValue();
    assertEquals(WEB_PUSH, delivery.getChannel());
    delivery.setLockToken("claim");
    when(repository.reschedule(anyString(), anyString(), anyInt(), any(), any(), any()))
        .thenReturn(1);
    when(repository.markSuccess(anyString(), anyString(), any())).thenReturn(1);
    var worker =
        new ExternalNotificationDeliveryWorker(
            repository, List.of(push), properties(), transaction, mock(NotificationMapper.class));
    var beforeAttempt = LocalDateTime.now();
    assertDoesNotThrow(() -> worker.deliverClaimed(delivery, () -> notification));
    var retryAt = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(repository)
        .reschedule(
            eq(delivery.getId()),
            eq("claim"),
            eq(1),
            retryAt.capture(),
            eq("push unavailable"),
            any());
    assertTrue(retryAt.getValue().isAfter(beforeAttempt.plusSeconds(1)));
    verify(repository, never()).markSuccess(anyString(), anyString(), any());
    worker.deliverClaimed(delivery, () -> notification);
    verify(repository).markSuccess(eq(delivery.getId()), eq("claim"), any());
    verify(push, times(2)).send(notification);
    verify(notifications).save(any());
    verify(socket).sendNotificationToUser("user", notification);
  }

  @Test
  void webPushWithNoSubscriptionsIsMarkedSuccessful() {
    var repository = mock(ExternalNotificationDeliveryRepository.class);
    when(repository.markSuccess(anyString(), anyString(), any())).thenReturn(1);
    var subscriptions = mock(WebPushSubscriptionRepository.class);
    when(subscriptions.findAllByUserId("user")).thenReturn(List.of());
    var sender = mock(WebPushSender.class);
    var push =
        new WebPushNotificationChannel(
            subscriptions,
            sender,
            mock(WebPushEndpointValidator.class),
            new WebPushProperties(),
            new NotificationMessageResolver(),
            new ObjectMapper(),
            mock(TransactionTemplate.class));
    var worker =
        new ExternalNotificationDeliveryWorker(
            repository,
            List.of(push),
            properties(),
            mock(TransactionTemplate.class),
            mock(NotificationMapper.class));
    var delivery = pendingDelivery();
    delivery.setChannel(WEB_PUSH);
    delivery.setId("notification-1:WEB_PUSH");
    var notification =
        Notification.builder()
            .id("notification-1")
            .recipient(Seller.builder().id("user").userType(UserType.SELLER).build())
            .build();

    worker.deliverClaimed(delivery, () -> notification);

    verify(repository).markSuccess(eq("notification-1:WEB_PUSH"), eq("claim"), any());
    verify(repository, never()).reschedule(anyString(), anyString(), anyInt(), any(), any(), any());
    verifyNoInteractions(sender);
  }

  private ExternalNotificationDeliveryProperties properties() {
    return new ExternalNotificationDeliveryProperties(
        100, Duration.ofSeconds(2), Duration.ofMinutes(5), Duration.ofSeconds(30));
  }

  private JExternalNotificationDelivery pendingDelivery() {
    var now = LocalDateTime.of(2026, 1, 1, 0, 0);
    return JExternalNotificationDelivery.builder()
        .id("notification-1:EMAIL")
        .channel(EMAIL)
        .status(ProcessStatus.PROCESSING)
        .attemptNb(0)
        .nextAttemptAt(now)
        .createdAt(now)
        .lockedAt(now)
        .lockToken("claim")
        .build();
  }
}
