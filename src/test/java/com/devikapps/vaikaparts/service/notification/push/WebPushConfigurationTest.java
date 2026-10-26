package com.devikapps.vaikaparts.service.notification.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.devikapps.vaikaparts.repository.WebPushSubscriptionRepository;
import com.devikapps.vaikaparts.service.notification.NotificationMessageResolver;
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
}
