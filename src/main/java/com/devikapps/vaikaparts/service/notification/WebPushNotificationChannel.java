package com.devikapps.vaikaparts.service.notification;

import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.WebPushSubscriptionRepository;
import com.devikapps.vaikaparts.service.notification.push.WebPushEndpointValidator;
import com.devikapps.vaikaparts.service.notification.push.WebPushPayload;
import com.devikapps.vaikaparts.service.notification.push.WebPushSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionTemplate;

@RequiredArgsConstructor
public class WebPushNotificationChannel implements NotificationChannel {
  private final WebPushSubscriptionRepository repository;
  private final WebPushSender sender;
  private final WebPushEndpointValidator validator;
  private final WebPushProperties properties;
  private final NotificationMessageResolver messageResolver;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;

  @Override
  public void send(Notification notification) {
    var subscriptions = repository.findAllByUserId(notification.getRecipient().getId());
    if (subscriptions.isEmpty()) return;
    byte[] payload = WebPushPayload.from(notification, messageResolver, objectMapper);
    boolean failed = false;
    for (var subscription : subscriptions) {
      try {
        validator.validate(subscription.getEndpoint());
        int status = sender.send(subscription, payload);
        if (status == 404 || status == 410) {
          transactionTemplate.executeWithoutResult(
              ignored ->
                  repository.deleteByEndpointAndUserId(
                      subscription.getEndpoint(), notification.getRecipient().getId()));
        } else if (status < 200 || status >= 300) {
          failed = true;
        }
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Web push delivery interrupted; retry required");
      } catch (Exception exception) {
        // Transport exceptions can contain subscription URLs or authentication material.
        failed = true;
      }
    }
    if (failed) throw new IllegalStateException("Web push delivery failed; retry required");
  }

  @Override
  public NotificationChannelType getChannelType() {
    return NotificationChannelType.WEB_PUSH;
  }

  // This bean is only created after VAPID validation succeeds at startup.
  @Override
  public boolean isEnabled() {
    return properties.isEnabled();
  }
}
