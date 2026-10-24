package com.devikapps.vaikaparts.service.notification;

import static com.devikapps.vaikaparts.model.classifier.NotificationType.OFFER_REJECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OfferRejectedNotificationListenerTest {

  @Test
  void creates_the_rejected_offer_notification_for_the_closed_offers_seller() {
    var notificationService = mock(NotificationService.class);
    var listener = new OfferRejectedNotificationListener(notificationService);

    listener.onRejectedOffer(new OfferRejectedNotificationRequested("offer", "seller"));

    var request = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(notificationService).createAndSendNotification(request.capture());
    assertEquals("seller", request.getValue().getRecipientUserId());
    assertEquals("offer", request.getValue().getResourceId());
    assertEquals(OFFER_REJECTED, request.getValue().getNotificationType());
    assertEquals("Votre offre n'a pas été retenue.", request.getValue().getMessage());
    assertEquals("/offers/offer", request.getValue().getClickAction());
  }
}
