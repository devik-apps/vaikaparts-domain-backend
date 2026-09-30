package com.devikapps.vaikaparts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.devikapps.vaikaparts.config.NotificationChannelsConf;
import com.devikapps.vaikaparts.mail.Mailer;
import com.devikapps.vaikaparts.service.notification.EmailNotificationChannel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NotificationChannelsConfTest {
  @Test
  void email_is_not_registered_by_default() {
    new ApplicationContextRunner()
        .withUserConfiguration(NotificationChannelsConf.class)
        .run(c -> assertThat(c).hasNotFailed().doesNotHaveBean(EmailNotificationChannel.class));
  }

  @Test
  void enabling_email_registers_channel_without_sending() {
    var mailer = mock(Mailer.class);
    new ApplicationContextRunner()
        .withUserConfiguration(NotificationChannelsConf.class)
        .withBean(Mailer.class, () -> mailer)
        .withPropertyValues("notifications.email.enabled=true")
        .run(
            c -> {
              assertThat(c).hasNotFailed().hasSingleBean(EmailNotificationChannel.class);
              assertThat(c.getBean(EmailNotificationChannel.class).isEnabled()).isTrue();
              verifyNoInteractions(mailer);
            });
  }
}
