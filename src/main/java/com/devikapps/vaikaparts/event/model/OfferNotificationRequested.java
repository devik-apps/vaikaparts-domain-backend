package com.devikapps.vaikaparts.event.model;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfferNotificationRequested extends InfraEvent {

  private static final Duration MAX_CONSUMER_DURATION = ofMinutes(2L);
  private static final Duration MAX_CONSUMER_BACKOFF = ofSeconds(15L);

  private final String id;

  @JsonProperty("researcher_id")
  private final String researcherId;

  @JsonProperty("offer_id")
  private final String offerId;

  @JsonCreator
  public OfferNotificationRequested(
      @JsonProperty("id") String id,
      @JsonProperty("researcher_id") String researcherId,
      @JsonProperty("offer_id") String offerId) {
    this.id = id;
    this.researcherId = researcherId;
    this.offerId = offerId;
  }

  @Override
  public Duration maxConsumerDuration() {
    return MAX_CONSUMER_DURATION;
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return MAX_CONSUMER_BACKOFF;
  }
}
