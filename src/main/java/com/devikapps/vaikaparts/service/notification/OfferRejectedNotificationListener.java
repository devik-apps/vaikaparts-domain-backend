package com.devikapps.vaikaparts.service.notification;

import static com.devikapps.vaikaparts.model.classifier.NotificationType.OFFER_REJECTED;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OfferRejectedNotificationListener {

  private final NotificationService notificationService;

  @Transactional(propagation = REQUIRES_NEW)
  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void onRejectedOffer(OfferRejectedNotificationRequested event) {
    notificationService.createAndSendNotification(
        NotificationRequest.builder()
            .recipientUserId(event.sellerId())
            .resourceId(event.offerId())
            .notificationType(OFFER_REJECTED)
            .message("Votre offre n'a pas été retenue.")
            .clickAction("/offers/" + event.offerId())
            .build());
  }
}
