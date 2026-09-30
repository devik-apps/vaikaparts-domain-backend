package com.devikapps.vaikaparts.service.notification;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.sms.SmsMessage;
import com.devikapps.vaikaparts.sms.SmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Enabled globally through SmsConf; user preferences are checked by the service. */
@Slf4j
@RequiredArgsConstructor
public class SmsNotificationChannel implements NotificationChannel {
  private static final String BRAND_TITLE = "VAIKAPARTS";

  private final SmsProvider provider;
  private final boolean enabled;

  @Override
  public void send(Notification notification) {
    if (!enabled) throw new IllegalStateException("SMS notification channel is disabled");
    if (notification == null || notification.getRecipient() == null) {
      throw new IllegalArgumentException("A notification recipient is required");
    }
    provider.send(
        new SmsMessage(
            notification.getRecipient().getPhoneNumber(),
            BRAND_TITLE + "\n" + notification.getMessage()));
    log.info(
        "[NOTIF-PIPELINE][SMS_ACCEPTED] notificationId={}, recipientType={} (not delivery"
            + " confirmation)",
        forJava(notification.getId()),
        notification.getRecipient().getUserType());
  }

  @Override
  public NotificationChannelType getChannelType() {
    return NotificationChannelType.SMS;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
