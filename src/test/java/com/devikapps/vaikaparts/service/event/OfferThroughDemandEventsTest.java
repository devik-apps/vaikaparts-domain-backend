package com.devikapps.vaikaparts.service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.event.model.*;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.classifier.*;
import com.devikapps.vaikaparts.repository.*;
import com.devikapps.vaikaparts.repository.event.*;
import com.devikapps.vaikaparts.repository.model.exchange.*;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import com.devikapps.vaikaparts.service.outbox.OutboxService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OfferThroughDemandEventsTest {
  @Test
  void persists_parent_and_enqueues_one_researcher_child() {
    var parents = mock(DemandPublishedRequestedRepository.class);
    var demands = mock(DemandRepository.class);
    var offers = mock(OfferRepository.class);
    var users = mock(UserRepository.class);
    var children = mock(NotificationRequestedRepository.class);
    var outbox = mock(OutboxService.class);
    var demand =
        JDemand.builder()
            .id("demand")
            .researcher(JResearcher.builder().id("researcher").build())
            .build();
    var offer = JOffer.builder().id("offer").demand(demand).build();
    when(demands.findByIdWithRelations("demand")).thenReturn(Optional.of(demand));
    when(offers.findById("offer")).thenReturn(Optional.of(offer));
    when(parents.save(any())).thenAnswer(i -> i.getArgument(0));
    var service =
        new DemandPublishedRequestedService(
            parents, demands, offers, users, mock(SellerMapper.class), children, outbox);
    service.accept(
        DemandPublishedRequested.builder()
            .id("parent")
            .demandId("demand")
            .offerId("offer")
            .build());
    verifyNoInteractions(users);
    verify(parents, atLeastOnce()).save(any());
    var saved = ArgumentCaptor.forClass(JDemandPublishedNotificationRequested.class);
    verify(children).save(saved.capture());
    var persistedChild = saved.getValue();
    assertEquals("parent", persistedChild.getDemandPublishedRequested().getId());
    assertEquals("offer", persistedChild.getOffer().getId());
    assertEquals("researcher", persistedChild.getResearcher().getId());
    assertNull(persistedChild.getSeller());
    var events = ArgumentCaptor.forClass(DemandPublishedNotificationRequested.class);
    verify(outbox).enqueue(events.capture());
    var child = events.getValue();
    assertEquals(persistedChild.getId(), child.getId());
    assertEquals("parent", child.getDemandPublishedRequestedId());
    assertEquals("offer", child.getOfferId());
    assertEquals("researcher", child.getResearcherId());
    assertNull(child.getSellerId());
  }

  @Test
  void persists_offer_child_before_creating_researcher_notification() {
    var children = mock(NotificationRequestedRepository.class);
    var parents = mock(DemandPublishedRequestedRepository.class);
    var offers = mock(OfferRepository.class);
    var notifications = mock(NotificationService.class);
    var researcher = JResearcher.builder().id("researcher").build();
    var demand = JDemand.builder().id("demand").researcher(researcher).build();
    var offer = JOffer.builder().id("offer").demand(demand).build();
    when(parents.findById("parent"))
        .thenReturn(
            Optional.of(JDemandPublishedRequested.builder().id("parent").demand(demand).build()));
    when(offers.findById("offer")).thenReturn(Optional.of(offer));
    var service =
        new DemandPublishedNotificationRequestedService(
            children,
            parents,
            mock(DemandRepository.class),
            offers,
            mock(SellerMapper.class),
            mock(UserRepository.class),
            notifications);
    service.accept(
        DemandPublishedNotificationRequested.builder()
            .id("child")
            .demandPublishedRequestedId("parent")
            .demandId("demand")
            .offerId("offer")
            .researcherId("researcher")
            .build());
    var order = inOrder(children, notifications);
    order.verify(children).save(any(JDemandPublishedNotificationRequested.class));
    var request = ArgumentCaptor.forClass(NotificationRequest.class);
    order.verify(notifications).createAndSendNotification(request.capture());
    assertEquals("researcher", request.getValue().getRecipientUserId());
    assertEquals("offer", request.getValue().getResourceId());
    assertEquals("child", request.getValue().getNotificationRequestedId());
    assertEquals(NotificationType.OFFER_PUBLISHED, request.getValue().getNotificationType());
    verify(children, atLeastOnce())
        .save(
            argThat(
                row ->
                    row.getOffer() == offer
                        && row.getResearcher() == researcher
                        && row.getSeller() == null
                        && row.getDemand() == null));
  }
}
