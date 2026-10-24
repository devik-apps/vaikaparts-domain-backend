package com.devikapps.vaikaparts.service.notification.delivery;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification-delivery")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExternalNotificationDeliveryProperties {
  private int batchSize = 100;
  private Duration initialBackoff = Duration.ofSeconds(2);
  private Duration maxBackoff = Duration.ofMinutes(5);
  private Duration leaseDuration = Duration.ofSeconds(30);
}
