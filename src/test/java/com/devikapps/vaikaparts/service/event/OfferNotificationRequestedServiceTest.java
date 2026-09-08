package com.devikapps.vaikaparts.service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.event.model.InfraEvent;
import com.devikapps.vaikaparts.event.model.OfferNotificationRequested;
import com.devikapps.vaikaparts.exception.OfferNotificationRequestedException;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.event.JNotificationRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OfferNotificationRequestedServiceTest {
  @Mock NotificationRequestedRepository repository;
  @Mock OfferRepository offerRepository;
  @Mock NotificationService notificationService;
  @InjectMocks OfferNotificationRequestedService service;
  @Captor ArgumentCaptor<NotificationRequest> requests;
  @Captor ArgumentCaptor<JNotificationRequested> logs;
  OfferNotificationRequested event;
  JOffer offer;

  @BeforeEach
  void setUp() {
    event =
        OfferNotificationRequested.builder()
            .id("event")
            .offerId("offer")
            .researcherId("researcher")
            .build();
    offer =
        JOffer.builder()
            .id("offer")
            .demand(
                JDemand.builder()
                    .id("demand")
                    .researcher(JResearcher.builder().id("researcher").build())
                    .part(
                        JPart.builder()
                            .partName("Headlight")
                            .carBrand("Toyota")
                            .carModel("Corolla")
                            .build())
                    .build())
            .build();
  }

  @Test
  void creates_one_request_without_parent_and_notifies_researcher() throws Exception {
    when(offerRepository.findByIdWithRelations("offer")).thenReturn(Optional.of(offer));
    service.accept(event);
    verify(notificationService).createAndSendNotification(requests.capture());
    var request = requests.getValue();
    assertEquals("researcher", request.getRecipientUserId());
    assertEquals("offer", request.getResourceId());
    assertEquals("event", request.getNotificationRequestedId());
    assertEquals(NotificationType.OFFER_PUBLISHED, request.getNotificationType());
    var action = new ObjectMapper().readTree(request.getClickAction());
    assertEquals("VIEW_OFFER", action.get("action").asText());
    assertEquals("offer", action.get("offerId").asText());
    assertEquals("demand", action.get("demandId").asText());
    verify(repository, times(2)).save(logs.capture());
    var log = logs.getValue();
    assertNull(log.getDemandPublishedRequested());
    assertNull(log.getSeller());
    assertNull(log.getDemand());
    assertEquals("researcher", log.getResearcher().getId());
    assertEquals("offer", log.getOffer().getId());
    assertEquals(ProcessStatus.SUCCESS, log.getStatus());
    assertNotNull(log.getCompletedAt());
  }

  @Test
  void completed_event_is_not_sent_again() {
    when(repository.findById("event"))
        .thenReturn(
            Optional.of(
                JNotificationRequested.builder()
                    .id("event")
                    .offer(offer)
                    .researcher(offer.getDemand().getResearcher())
                    .status(ProcessStatus.SUCCESS)
                    .build()));
    service.accept(event);
    verifyNoInteractions(notificationService, offerRepository);
    verify(repository, never()).save(any());
  }

  @Test
  void recipient_must_own_the_demand() {
    when(offerRepository.findByIdWithRelations("offer")).thenReturn(Optional.of(offer));
    var invalid =
        OfferNotificationRequested.builder()
            .id("event")
            .offerId("offer")
            .researcherId("someone-else")
            .build();
    assertThrows(IllegalArgumentException.class, () -> service.accept(invalid));
    verifyNoInteractions(notificationService);
    verify(repository, never()).save(any());
  }

  @Test
  void missing_offer_does_not_notify() {
    assertThrows(IllegalStateException.class, () -> service.accept(event));
    verifyNoInteractions(notificationService);
  }

  @Test
  void records_failure_and_propagates_error() {
    when(offerRepository.findByIdWithRelations("offer")).thenReturn(Optional.of(offer));
    when(notificationService.createAndSendNotification(any()))
        .thenThrow(new IllegalStateException("failed"));
    event.setAttemptNb(2);
    assertThrows(OfferNotificationRequestedException.class, () -> service.accept(event));
    verify(repository, times(2)).save(logs.capture());
    assertEquals(ProcessStatus.FAILED, logs.getValue().getStatus());
    assertEquals("failed", logs.getValue().getErrorMessage());
    assertEquals(2, logs.getValue().getAttemptNb());
  }

  @Test
  void retry_reuses_log_and_clears_previous_error() {
    var log =
        JNotificationRequested.builder()
            .id("event")
            .offer(offer)
            .researcher(offer.getDemand().getResearcher())
            .status(ProcessStatus.FAILED)
            .errorMessage("previous failure")
            .attemptNb(1)
            .build();
    when(repository.findById("event")).thenReturn(Optional.of(log));
    event.setAttemptNb(2);

    service.accept(event);

    assertEquals(ProcessStatus.SUCCESS, log.getStatus());
    assertNull(log.getErrorMessage());
    assertEquals(2, log.getAttemptNb());
    verify(notificationService).createAndSendNotification(any());
    verifyNoInteractions(offerRepository);
  }

  @Test
  void rabbitmq_payload_round_trip_preserves_recipient_and_attempt() throws Exception {
    var mapper = new ObjectMapper();
    mapper.registerSubtypes(
        new NamedType(OfferNotificationRequested.class, "OfferNotificationRequested"));
    event.setAttemptNb(3);
    var decoded =
        (OfferNotificationRequested)
            mapper.readValue(mapper.writeValueAsString(event), InfraEvent.class);
    assertEquals(event.getId(), decoded.getId());
    assertEquals(event.getOfferId(), decoded.getOfferId());
    assertEquals(event.getResearcherId(), decoded.getResearcherId());
    assertEquals(3, decoded.getAttemptNb());
  }
}
