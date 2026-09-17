package com.devikapps.vaikaparts.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.exception.EmailSendException;
import com.devikapps.vaikaparts.exception.NotificationDeliveryException;
import com.devikapps.vaikaparts.mail.Email;
import com.devikapps.vaikaparts.mail.Mailer;
import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.service.notification.EmailNotificationChannel;
import com.devikapps.vaikaparts.service.notification.SmsNotificationChannel;
import com.devikapps.vaikaparts.sms.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

class ExternalNotificationChannelsTest {
  private Notification notification(NotificationType type) {
    return Notification.builder().id("notification").notificationType(type)
        .recipient(Researcher.builder().email("alice@example.com").phoneNumber("0321234567").build())
        .message("Une pièce <script>alert('test')</script> & disponible").build();
  }

  @ParameterizedTest
  @EnumSource(NotificationType.class)
  void email_uses_safe_html_and_subject_for_every_type(NotificationType type) {
    var mailer = mock(Mailer.class);
    var channel = new EmailNotificationChannel(mailer, true);
    channel.send(notification(type));
    var capture = ArgumentCaptor.forClass(Email.class);
    verify(mailer).sendOrThrow(capture.capture());
    assertEquals("alice@example.com", capture.getValue().to().getAddress());
    assertTrue(capture.getValue().subject().startsWith("VaikaParts"));
    assertFalse(capture.getValue().htmlBody().contains("<script>"));
    assertTrue(capture.getValue().htmlBody().contains("&lt;script&gt;"));
    assertEquals(NotificationChannelType.EMAIL, channel.getChannelType());
  }

  @Test
  void invalid_email_never_reaches_mailer() {
    var mailer = mock(Mailer.class);
    var notification = notification(NotificationType.OFFER_PUBLISHED);
    notification.getRecipient().setEmail("invalid\r\nBcc: other@example.com");
    assertThrows(NotificationDeliveryException.class,
        () -> new EmailNotificationChannel(mailer, true).send(notification));
    verifyNoInteractions(mailer);
  }

  @Test
  void email_failure_is_propagated() {
    var mailer = mock(Mailer.class);
    doThrow(new EmailSendException("SMTP failed")).when(mailer).sendOrThrow(any());
    assertThrows(EmailSendException.class,
        () -> new EmailNotificationChannel(mailer, true).send(notification(NotificationType.OFFER_PUBLISHED)));
  }

  @Test
  void sms_channel_only_depends_on_generic_provider() {
    var provider = mock(SmsProvider.class);
    var channel = new SmsNotificationChannel(provider, true);
    var notification = notification(NotificationType.OFFER_PUBLISHED);
    channel.send(notification);
    verify(provider).send(new SmsMessage("0321234567", notification.getMessage()));
    assertEquals(NotificationChannelType.SMS, channel.getChannelType());
    when(provider.send(any())).thenThrow(new SmsSendException(SmsSendException.Reason.ACCOUNT_UNAVAILABLE, 403));
    assertThrows(SmsSendException.class, () -> channel.send(notification));
  }

  @Test
  void disabled_channels_do_not_send() {
    var provider = mock(SmsProvider.class);
    var mailer = mock(Mailer.class);
    var sms = new SmsNotificationChannel(provider, false);
    var email = new EmailNotificationChannel(mailer, false);
    assertFalse(sms.isEnabled());
    assertFalse(email.isEnabled());
    assertThrows(IllegalStateException.class, () -> sms.send(notification(NotificationType.OFFER_PUBLISHED)));
    assertThrows(IllegalStateException.class, () -> email.send(notification(NotificationType.OFFER_PUBLISHED)));
    verifyNoInteractions(provider, mailer);
  }
}
