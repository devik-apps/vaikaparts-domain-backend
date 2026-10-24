package com.devikapps.vaikaparts.service.notification;

import static com.devikapps.vaikaparts.model.classifier.PostStatus.CLOSED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OfferOutcomeNotificationPublisherTest {

  @Test
  void publishes_one_rejection_for_each_newly_closed_offer_to_its_seller() {
    var published = new ArrayList<Object>();
    var publisher = new OfferOutcomeNotificationPublisher(published::add);
    var sellerA = JSeller.builder().id("seller-a").build();
    var sellerB = JSeller.builder().id("seller-b").build();
    var closedA = JOffer.builder().id("offer-a").seller(sellerA).status(CLOSED).build();
    var closedB = JOffer.builder().id("offer-b").seller(sellerB).status(CLOSED).build();

    publisher.publishRejectedOffers(List.of(closedA, closedB));

    assertEquals(
        List.of(
            new OfferRejectedNotificationRequested("offer-a", "seller-a"),
            new OfferRejectedNotificationRequested("offer-b", "seller-b")),
        published);
  }
}
