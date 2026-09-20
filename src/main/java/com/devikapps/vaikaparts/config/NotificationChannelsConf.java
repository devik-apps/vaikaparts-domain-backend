package com.devikapps.vaikaparts.config;

import com.devikapps.vaikaparts.mail.Mailer;
import com.devikapps.vaikaparts.service.notification.EmailNotificationChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationChannelsConf {
  @Bean
  @ConditionalOnProperty(name = "notifications.email.enabled", havingValue = "true")
  public EmailNotificationChannel emailNotificationChannel(Mailer mailer) {
    return new EmailNotificationChannel(mailer, true);
  }
}
