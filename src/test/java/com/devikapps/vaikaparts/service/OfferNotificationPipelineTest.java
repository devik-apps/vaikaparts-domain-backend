package com.devikapps.vaikaparts.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.config.JacksonConf;
import com.devikapps.vaikaparts.datastructure.ListGrouper;
import com.devikapps.vaikaparts.endpoint.rest.controller.exchange.OfferController;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange.CreateOfferRequest;
import com.devikapps.vaikaparts.event.config.InfraEventTypeRegistrar;
import com.devikapps.vaikaparts.event.consumer.EventConsumer;
import com.devikapps.vaikaparts.event.consumer.EventDispatcher;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.event.model.OfferNotificationRequested;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.repository.OfferNotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.service.event.OfferNotificationRequestedService;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

class OfferNotificationPipelineTest {
  @Test
  void production_json_passes_through_producer_consumer_dispatcher_and_offer_handler() {
    var mapper = new JacksonConf().objectMapper();
    new InfraEventTypeRegistrar(mapper).registerTypes();
    var rabbit = mock(RabbitTemplate.class);
    var producer =
        new EventProducer<OfferNotificationRequested>(
            rabbit, mapper, "exchange", "routing", new ListGrouper<>());
    var repository = mock(OfferNotificationRequestedRepository.class);
    var offers = mock(OfferRepository.class);
    var notifications = mock(NotificationService.class);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(offers.findByIdWithRelations("offer"))
        .thenReturn(
            Optional.of(
                JOffer.builder()
                    .id("offer")
                    .demand(
                        JDemand.builder()
                            .id("demand")
                            .researcher(JResearcher.builder().id("researcher").build())
                            .part(JPart.builder().partName("Headlight").build())
                            .build())
                    .build()));
    var handler = new OfferNotificationRequestedService(repository, offers, notifications);
    try (var context = new StaticApplicationContext()) {
      context.getBeanFactory().registerSingleton("offerNotificationRequestedService", handler);
      var dispatcher = new EventDispatcher();
      dispatcher.setApplicationContext(context);
      var consumer = new EventConsumer(dispatcher, mapper);
      try {
        // RabbitMQ itself is mocked; production serialization and dispatch are exercised.
        doAnswer(
                invocation -> {
                  consumer.onMessage(invocation.getArgument(2, String.class));
                  return null;
                })
            .when(rabbit)
            .convertAndSend(eq("exchange"), eq("routing"), any(Object.class));
        producer.accept(
            List.of(
                OfferNotificationRequested.builder()
                    .id("event")
                    .offerId("offer")
                    .researcherId("researcher")
                    .build()));

        var request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notifications, timeout(5000)).createAndSendNotification(request.capture());
        assertEquals("researcher", request.getValue().getRecipientUserId());
        assertEquals("offer", request.getValue().getResourceId());
        verify(repository, timeout(5000).atLeastOnce())
            .save(argThat(log -> log.getStatus() == ProcessStatus.SUCCESS));
      } finally {
        ((ExecutorService) ReflectionTestUtils.getField(consumer, "executor")).shutdownNow();
      }
    }
  }

  @Test
  void create_returns_published_offer_and_explicit_draft_does_not_publish() {
    var service = mock(OfferService.class);
    var request = mock(CreateOfferRequest.class);
    when(request.demandId()).thenReturn("demand");
    when(request.description()).thenReturn("description");
    var draft = Offer.builder().id("offer").status(PostStatus.DRAFT).build();
    var published = Offer.builder().id("offer").status(PostStatus.PUBLISHED).build();
    when(service.createOffer("demand", "description", null)).thenReturn(draft);
    when(service.updateOfferStatus("offer", PostStatus.PUBLISHED)).thenReturn(published);
    var controller = new OfferController(service);
    assertSame(published, controller.createOffer(request, true).getBody());
    clearInvocations(service);
    assertSame(draft, controller.createOffer(request, false).getBody());
    verify(service, never()).updateOfferStatus(anyString(), any());
  }
}
