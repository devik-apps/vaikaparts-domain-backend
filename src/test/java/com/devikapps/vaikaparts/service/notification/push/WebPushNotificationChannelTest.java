package com.devikapps.vaikaparts.service.notification.push;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.config.JacksonConf;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.UserLanguage;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.repository.WebPushSubscriptionRepository;
import com.devikapps.vaikaparts.repository.model.notification.JWebPushSubscription;
import com.devikapps.vaikaparts.service.notification.NotificationMessageResolver;
import com.devikapps.vaikaparts.service.notification.WebPushNotificationChannel;
import com.devikapps.vaikaparts.service.notification.WebPushProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WebPushNotificationChannelTest {
  final WebPushSubscriptionRepository repository = mock(WebPushSubscriptionRepository.class);
  final WebPushSender sender = mock(WebPushSender.class);
  final WebPushEndpointValidator validator = mock(WebPushEndpointValidator.class);
  final WebPushProperties properties = new WebPushProperties();
  final ObjectMapper mapper = new JacksonConf().objectMapper();
  final org.springframework.transaction.PlatformTransactionManager transactionManager =
      mock(org.springframework.transaction.PlatformTransactionManager.class);
  final WebPushNotificationChannel channel =
      new WebPushNotificationChannel(
          repository,
          sender,
          validator,
          properties,
          new NotificationMessageResolver(),
          mapper,
          new org.springframework.transaction.support.TransactionTemplate(transactionManager));

  Notification notification(NotificationType type) {
    return Notification.builder()
        .id("notification-1")
        .notificationType(type)
        .message("Localized announcement")
        .recipient(
            Researcher.builder()
                .id("user-1")
                .email("secret@example.com")
                .preferredLanguage(UserLanguage.EN)
                .build())
        .clickAction("/notifications")
        .build();
  }

  JWebPushSubscription subscription(String name) {
    return JWebPushSubscription.builder()
        .endpoint("https://push.example.com/" + name)
        .p256dh("key")
        .auth("auth")
        .build();
  }

  @Test
  void sendWithNoSubscriptionsSucceeds() {
    when(repository.findAllByUserId("user-1")).thenReturn(List.of());
    assertDoesNotThrow(() -> channel.send(notification(NotificationType.OFFER_ACCEPTED)));
    verifyNoInteractions(sender);
  }

  @Test
  void sendFansOutToAllUserSubscriptions() throws Exception {
    var a = subscription("a");
    var b = subscription("b");
    when(repository.findAllByUserId("user-1")).thenReturn(List.of(a, b));
    when(sender.send(any(), any())).thenReturn(201);
    channel.send(notification(NotificationType.OFFER_ACCEPTED));
    var order = inOrder(validator, sender);
    order.verify(validator).validate(a.getEndpoint());
    order.verify(sender).send(eq(a), any());
    order.verify(validator).validate(b.getEndpoint());
    order.verify(sender).send(eq(b), any());
  }

  @Test
  void payloadMappingCoversEveryNotificationType() throws Exception {
    when(repository.findAllByUserId("user-1")).thenReturn(List.of(subscription("a")));
    when(sender.send(any(), any())).thenReturn(201);
    for (var type : NotificationType.values()) channel.send(notification(type));
    var payloads = ArgumentCaptor.forClass(byte[].class);
    verify(sender, times(NotificationType.values().length)).send(any(), payloads.capture());
    for (int i = 0; i < NotificationType.values().length; i++) {
      var json = mapper.readTree(payloads.getAllValues().get(i));
      var keys = new HashSet<String>();
      json.fieldNames().forEachRemaining(keys::add);
      assertEquals(Set.of("id", "type", "message", "click_action"), keys);
      assertEquals("notification-1", json.get("id").asText());
      assertEquals(NotificationType.values()[i].name(), json.get("type").asText());
      assertFalse(json.get("message").asText().isBlank());
      assertEquals("/notifications", json.get("click_action").asText());
      assertFalse(json.toString().contains("secret@example.com"));
    }
    assertEquals(
        "Your offer has been accepted",
        mapper.readTree(payloads.getAllValues().get(2)).get("message").asText());
  }

  @Test
  void expiredEndpointIsDeletedAndOthersStillReceive() throws Exception {
    var a = subscription("a");
    var b = subscription("b");
    var c = subscription("c");
    when(repository.findAllByUserId("user-1")).thenReturn(List.of(a, b, c));
    when(sender.send(eq(a), any())).thenReturn(404);
    when(sender.send(eq(b), any())).thenReturn(410);
    when(sender.send(eq(c), any())).thenReturn(201);
    channel.send(notification(NotificationType.OFFER_ACCEPTED));
    verify(repository).deleteByEndpointAndUserId(a.getEndpoint(), "user-1");
    verify(repository).deleteByEndpointAndUserId(b.getEndpoint(), "user-1");
    verify(sender).send(eq(c), any());
    verify(transactionManager, times(2)).commit(any());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {301, 307, 400, 401, 403, 429, 500, 503})
  void transientFailureIsPropagatedForRetry(int status) throws Exception {
    var a = subscription("a");
    var b = subscription("b");
    when(repository.findAllByUserId("user-1")).thenReturn(List.of(a, b));
    when(sender.send(eq(a), any())).thenReturn(status);
    when(sender.send(eq(b), any())).thenReturn(201);
    var failure =
        assertThrows(
            RuntimeException.class,
            () -> channel.send(notification(NotificationType.OFFER_ACCEPTED)));
    assertFalse(failure.toString().contains(a.getEndpoint()));
    verify(sender).send(eq(b), any());
    verify(repository, never()).deleteByEndpointAndUserId(any(), any());
  }

  @Test
  void networkFailureContinuesFanOutWithoutLeakingEndpoint() throws Exception {
    var a = subscription("a");
    var b = subscription("b");
    when(repository.findAllByUserId("user-1")).thenReturn(List.of(a, b));
    when(sender.send(eq(a), any())).thenThrow(new IOException(a.getEndpoint()));
    when(sender.send(eq(b), any())).thenReturn(201);
    var error =
        assertThrows(
            RuntimeException.class,
            () -> channel.send(notification(NotificationType.OFFER_ACCEPTED)));
    assertNull(error.getCause());
    verify(sender).send(eq(b), any());
  }

  @Test
  void unsafeEndpointIsNotSentOrDeletedAndOthersStillReceive() throws Exception {
    var a = subscription("a");
    var b = subscription("b");
    when(repository.findAllByUserId("user-1")).thenReturn(List.of(a, b));
    doThrow(new IllegalArgumentException("unsafe")).when(validator).validate(a.getEndpoint());
    when(sender.send(eq(b), any())).thenReturn(201);
    assertThrows(
        RuntimeException.class, () -> channel.send(notification(NotificationType.OFFER_ACCEPTED)));
    verify(sender, never()).send(eq(a), any());
    verify(sender).send(eq(b), any());
    verify(repository, never()).deleteByEndpointAndUserId(any(), any());
  }

  @Test
  void payloadIsBoundedAndRejectsUnsafeClickActions() throws Exception {
    when(repository.findAllByUserId("user-1")).thenReturn(List.of(subscription("a")));
    when(sender.send(any(), any())).thenReturn(201);
    for (var action :
        List.of(
            "https://evil.example/",
            "//evil.example/",
            "/\\evil.example",
            "/%2f%2fevil.example",
            "/path\n",
            "/" + "a".repeat(3000))) {
      var n = notification(NotificationType.SYSTEM_ANNOUNCEMENT);
      n.setMessage("\"\n??".repeat(10000));
      n.setClickAction(action);
      channel.send(n);
    }
    var payloads = ArgumentCaptor.forClass(byte[].class);
    verify(sender, times(6)).send(any(), payloads.capture());
    for (var bytes : payloads.getAllValues()) {
      assertTrue(bytes.length <= 3072);
      assertEquals("/notifications", mapper.readTree(bytes).get("click_action").asText());
    }
  }
}
