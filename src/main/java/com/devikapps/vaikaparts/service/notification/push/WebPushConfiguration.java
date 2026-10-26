package com.devikapps.vaikaparts.service.notification.push;

import com.devikapps.vaikaparts.repository.WebPushSubscriptionRepository;
import com.devikapps.vaikaparts.service.notification.NotificationMessageResolver;
import com.devikapps.vaikaparts.service.notification.WebPushNotificationChannel;
import com.devikapps.vaikaparts.service.notification.WebPushProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
public class WebPushConfiguration {
  @Bean
  @ConditionalOnProperty(prefix = "notifications.web-push", name = "enabled", havingValue = "true")
  public LibraryWebPushSender webPushSender(
      WebPushProperties properties, WebPushEndpointValidator validator) {
    return new LibraryWebPushSender(
        properties,
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build(),
        validator);
  }

  @Bean
  @ConditionalOnProperty(prefix = "notifications.web-push", name = "enabled", havingValue = "true")
  public WebPushNotificationChannel webPushNotificationChannel(
      WebPushSubscriptionRepository repository,
      LibraryWebPushSender sender,
      WebPushEndpointValidator validator,
      WebPushProperties properties,
      NotificationMessageResolver resolver,
      ObjectMapper mapper,
      PlatformTransactionManager transactionManager) {
    var cleanupTransaction = new TransactionTemplate(transactionManager);
    cleanupTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return new WebPushNotificationChannel(
        repository, sender, validator, properties, resolver, mapper, cleanupTransaction);
  }
}
