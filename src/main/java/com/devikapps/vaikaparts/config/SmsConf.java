package com.devikapps.vaikaparts.config;

import com.devikapps.vaikaparts.service.notification.SmsNotificationChannel;
import com.devikapps.vaikaparts.sms.SmsProvider;
import com.devikapps.vaikaparts.sms.befiana.BefianaSmsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "notifications.sms.enabled", havingValue = "true")
public class SmsConf {
  @Bean
  public SmsProvider smsProvider(
      ObjectMapper mapper,
      @Value("${notifications.sms.befiana.base-url}") String baseUrl,
      @Value("${notifications.sms.befiana.api-key}") String apiKey,
      @Value("${notifications.sms.connect-timeout:5s}") String connectTimeout,
      @Value("${notifications.sms.request-timeout:10s}") String requestTimeout) {
    var httpClient =
        HttpClient.newBuilder()
            .connectTimeout(DurationStyle.detectAndParse(connectTimeout))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    try {
      return new BefianaSmsClient(
          httpClient, mapper, baseUrl, apiKey, DurationStyle.detectAndParse(requestTimeout));
    } catch (RuntimeException e) {
      httpClient.close();
      throw e;
    }
  }

  @Bean
  public SmsNotificationChannel smsNotificationChannel(SmsProvider provider) {
    return new SmsNotificationChannel(provider, true);
  }
}
