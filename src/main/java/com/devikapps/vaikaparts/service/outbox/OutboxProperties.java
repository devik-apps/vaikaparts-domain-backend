package com.devikapps.vaikaparts.service.outbox;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxProperties {
  private int batchSize = 100;
  private Duration confirmTimeout = Duration.ofSeconds(5);
  private Duration initialBackoff = Duration.ofSeconds(2);
  private Duration maxBackoff = Duration.ofMinutes(5);
  private Duration leaseDuration = Duration.ofSeconds(30);

  @PostConstruct
  void validate() {
    if (confirmTimeout.isNegative()
        || confirmTimeout.isZero()
        || leaseDuration.compareTo(confirmTimeout) <= 0) {
      throw new IllegalStateException("Outbox lease duration must be greater than confirm timeout");
    }
  }
}
