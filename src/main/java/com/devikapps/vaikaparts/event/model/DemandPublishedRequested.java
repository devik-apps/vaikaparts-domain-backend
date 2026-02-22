package com.devikapps.vaikaparts.event.model;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandPublishedRequested extends InfraEvent {

  private static final Duration MAX_CONSUMER_DURATION = ofMinutes(5L);
  private static final Duration MAX_CONSUMER_BACKOFF = ofSeconds(30L);

  private final String id;
  private final String demandId;

  @Override
  public Duration maxConsumerDuration() {
    return MAX_CONSUMER_DURATION;
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return MAX_CONSUMER_BACKOFF;
  }
}
