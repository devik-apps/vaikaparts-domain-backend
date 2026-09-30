package com.devikapps.vaikaparts.event.model;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("ContactReleaseRequested")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactReleaseRequested extends InfraEvent {

  private static final Duration MAX_CONSUMER_DURATION = ofMinutes(1);
  private static final Duration MAX_CONSUMER_BACKOFF = ofSeconds(30);

  private String id;
  private String paymentId;
  private String unlockRequestId;
  private String buyerId;
  private String sellerId;
  private String provider;
  private Instant paidAt;

  @Override
  public Duration maxConsumerDuration() {
    return MAX_CONSUMER_DURATION;
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return MAX_CONSUMER_BACKOFF;
  }

  @Override
  @JsonIgnore
  public String getEventSource() {
    return super.getEventSource();
  }
}
