package com.devikapps.vaikaparts.sms;

import static org.assertj.core.api.Assertions.assertThat;

import com.devikapps.vaikaparts.config.SmsConf;
import com.devikapps.vaikaparts.service.notification.SmsNotificationChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SmsConfTest {
  private final ApplicationContextRunner context =
      new ApplicationContextRunner()
          .withUserConfiguration(SmsConf.class)
          .withBean(ObjectMapper.class, () -> new ObjectMapper());

  @Test
  void disabled_provider_requires_no_credentials() {
    context.run(
        c ->
            assertThat(c)
                .hasNotFailed()
                .doesNotHaveBean(SmsProvider.class)
                .doesNotHaveBean(SmsNotificationChannel.class));
  }

  @Test
  void enabled_provider_accepts_configuration_without_sending() {
    context
        .withPropertyValues(
            "notifications.sms.enabled=true",
            "notifications.sms.befiana.base-url=https://sms.example.com",
            "notifications.sms.befiana.api-key=test-key",
            "notifications.sms.connect-timeout=5s",
            "notifications.sms.request-timeout=10s")
        .run(
            c ->
                assertThat(c)
                    .hasNotFailed()
                    .hasSingleBean(SmsProvider.class)
                    .hasSingleBean(SmsNotificationChannel.class));
  }

  @Test
  void enabled_provider_rejects_missing_api_key() {
    context
        .withPropertyValues(
            "notifications.sms.enabled=true",
            "notifications.sms.befiana.base-url=https://sms.example.com",
            "notifications.sms.befiana.api-key=",
            "notifications.sms.connect-timeout=PT5S",
            "notifications.sms.request-timeout=PT10S")
        .run(c -> assertThat(c).hasFailed());
  }
}
