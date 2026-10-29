package com.devikapps.vaikaparts.service.notification.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.devikapps.vaikaparts.repository.WebPushSubscriptionRepository;
import com.devikapps.vaikaparts.service.notification.NotificationMessageResolver;
import com.devikapps.vaikaparts.service.notification.NextWebPushNotificationChannel;
import com.devikapps.vaikaparts.service.notification.WebPushNotificationChannel;
import com.devikapps.vaikaparts.service.notification.WebPushProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class WebPushConfigurationTest {
  final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(WebPushConfiguration.class, WebPushProperties.class)
          .withBean(
              WebPushSubscriptionRepository.class, () -> mock(WebPushSubscriptionRepository.class))
          .withBean(WebPushEndpointValidator.class, () -> mock(WebPushEndpointValidator.class))
          .withBean(NotificationMessageResolver.class, NotificationMessageResolver::new)
          .withBean(ObjectMapper.class, ObjectMapper::new)
          .withBean(
              org.springframework.transaction.PlatformTransactionManager.class,
              () -> mock(org.springframework.transaction.PlatformTransactionManager.class));

  @Test
  void disabledModeDoesNotRequireKeys() {
    runner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).doesNotHaveBean(WebPushNotificationChannel.class);
        });
  }

  @Test
  void enabledModeWithValidKeysRegistersEnabledChannel() throws Exception {
    var p = LibraryWebPushSenderTest.validProperties();
    runner
        .withPropertyValues(
            "notifications.web-push.enabled=true",
            "notifications.web-push.public-key=" + p.getPublicKey(),
            "notifications.web-push.private-key=" + p.getPrivateKey(),
            "notifications.web-push.subject=" + p.getSubject())
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              var channel = context.getBean(WebPushNotificationChannel.class);
              assertThat(channel.isEnabled()).isTrue();
              assertThat(channel.getChannelType())
                  .isEqualTo(
                      com.devikapps.vaikaparts.model.classifier.NotificationChannelType.WEB_PUSH);
              context.getBean(WebPushProperties.class).setEnabled(false);
              assertThat(channel.isEnabled()).isFalse();
            });
  }

  @Test
  void enabledModeRequiresVapidKeys() {
    runner
        .withPropertyValues("notifications.web-push.enabled=true")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseMessage(
                      "Enabled web push requires valid VAPID public-key, private-key and subject");
            });
  }

  @Test
  void dispatchModeRegistersNextChannelInsteadOfJavaNativeChannel() {
    runner
        .withPropertyValues(
            "notifications.web-push.enabled=true",
            "notifications.web-push.dispatch-enabled=true",
            "notifications.web-push.dispatch-url=http://127.0.0.1:3000/api/push/dispatch",
            "notifications.web-push.dispatch-secret=0123456789abcdef0123456789abcdef")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(NextWebPushNotificationChannel.class);
              assertThat(context).doesNotHaveBean(WebPushNotificationChannel.class);
            });
  }

  @Test
  void dispatchModeRequiresAValidSharedSecret() {
    runner
        .withPropertyValues(
            "notifications.web-push.enabled=true",
            "notifications.web-push.dispatch-enabled=true",
            "notifications.web-push.dispatch-url=https://vaikaparts.com/api/push/dispatch",
            "notifications.web-push.dispatch-secret=short")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseMessage("PUSH_DISPATCH_SECRET must contain at least 32 characters");
            });
  }
}
