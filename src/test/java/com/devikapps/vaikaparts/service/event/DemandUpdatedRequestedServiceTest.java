package com.devikapps.vaikaparts.service.event;

import static com.devikapps.vaikaparts.model.classifier.PartCategory.ENGINE_PART;
import static com.devikapps.vaikaparts.model.classifier.UserStatus.ENABLED;
import static com.devikapps.vaikaparts.model.classifier.UserType.SELLER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.event.model.DemandUpdatedRequested;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemandUpdatedRequestedServiceTest {

  @Mock private DemandRepository demandRepository;
  @Mock private OfferRepository offerRepository;
  @Mock private UserRepository userRepository;
  @Mock private NotificationService notificationService;
  @InjectMocks private DemandUpdatedRequestedService service;

  @Test
  void notifies_category_sellers_and_existing_offer_sellers_once_without_sms() {
    var demandId = "demand-1";
    var event = DemandUpdatedRequested.builder().id("event-1").demandId(demandId).build();
    var demand =
        JDemand.builder()
            .id(demandId)
            .part(JPart.builder().partCategory(ENGINE_PART).build())
            .build();
    var matchingSeller =
        JSeller.builder()
            .id("seller-matching")
            .handleAllCategory(false)
            .categoryList(List.of(ENGINE_PART))
            .build();
    var activeOfferSeller =
        JSeller.builder()
            .id("seller-existing-offer")
            .handleAllCategory(false)
            .categoryList(List.of())
            .build();
    when(demandRepository.findByIdWithRelations(demandId)).thenReturn(Optional.of(demand));
    when(userRepository.findAllByUserTypeAndStatus(SELLER, ENABLED))
        .thenReturn(List.of(matchingSeller, activeOfferSeller));
    when(offerRepository.findDistinctSellerIdsByDemandId(demandId))
        .thenReturn(
            List.of("seller-matching", "seller-existing-offer", "seller-disabled-offer"));

    service.accept(event);
    service.accept(event);

    var requestCaptor = ArgumentCaptor.forClass(NotificationRequest.class);
    verify(notificationService, org.mockito.Mockito.times(4))
        .createAndSendNotification(requestCaptor.capture());
    var requests = requestCaptor.getAllValues();
    assertEquals(
        List.of(
            "seller-matching",
            "seller-existing-offer",
            "seller-matching",
            "seller-existing-offer"),
        requests.stream().map(NotificationRequest::getRecipientUserId).toList());
    assertEquals(NotificationType.DEMAND_UPDATED, requests.getFirst().getNotificationType());
    assertEquals(demandId, requests.getFirst().getResourceId());
    assertEquals(
        "{\"action\":\"VIEW_DEMAND\",\"demandId\":\"demand-1\"}",
        requests.getFirst().getClickAction());
    assertNull(requests.getFirst().getNotificationRequestedId());
    assertNotEquals(
        requests.get(0).getNotificationId(), requests.get(1).getNotificationId());
    assertEquals(requests.get(0).getNotificationId(), requests.get(2).getNotificationId());
    assertEquals(requests.get(1).getNotificationId(), requests.get(3).getNotificationId());
  }
}
