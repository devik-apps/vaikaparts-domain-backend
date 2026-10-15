package com.devikapps.vaikaparts.service.notification;

import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfferOutcomeNotificationPublisher {

  private final ApplicationEventPublisher eventPublisher;

  /** Publishes rejection notifications for offers that became closed in the current transaction. */
  public void publishRejectedOffers(List<JOffer> newlyClosedOffers) {
    newlyClosedOffers.forEach(
        offer ->
            eventPublisher.publishEvent(
                new OfferRejectedNotificationRequested(offer.getId(), offer.getSeller().getId())));
  }
}
