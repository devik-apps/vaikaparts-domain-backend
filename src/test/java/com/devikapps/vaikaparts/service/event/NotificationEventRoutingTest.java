package com.devikapps.vaikaparts.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.devikapps.vaikaparts.config.JacksonConf;
import com.devikapps.vaikaparts.event.config.InfraEventTypeRegistrar;
import com.devikapps.vaikaparts.event.consumer.EventConsumer;
import com.devikapps.vaikaparts.event.consumer.EventDispatcher;
import com.devikapps.vaikaparts.event.model.DemandPublishedNotificationRequested;
import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.event.model.InfraEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.StaticApplicationContext;

class NotificationEventRoutingTest {
  @Test
  void listener_returns_only_after_handler_finishes() throws Exception {
    var mapper = new JacksonConf().objectMapper();
    new InfraEventTypeRegistrar(mapper).registerTypes();
    var payload =
        mapper.writeValueAsString(
            DemandPublishedRequested.builder().id("parent").demandId("demand").build());
    var handlerStarted = new CountDownLatch(1);
    var allowHandlerToFinish = new CountDownLatch(1);
    Consumer<InfraEvent> handler =
        ignored -> {
          handlerStarted.countDown();
          try {
            assertTrue(allowHandlerToFinish.await(5, TimeUnit.SECONDS));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
          }
        };

    try (var context = new StaticApplicationContext()) {
      context.getBeanFactory().registerSingleton("demandPublishedRequestedService", handler);
      var dispatcher = new EventDispatcher();
      dispatcher.setApplicationContext(context);
      var consumer = new EventConsumer(dispatcher, mapper);

      var listenerCall = CompletableFuture.runAsync(() -> consumer.onMessage(payload));
      assertTrue(handlerStarted.await(5, TimeUnit.SECONDS));
      assertFalse(listenerCall.isDone());
      allowHandlerToFinish.countDown();
      listenerCall.get(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void listener_propagates_handler_failure_for_broker_retry() throws Exception {
    var mapper = new JacksonConf().objectMapper();
    new InfraEventTypeRegistrar(mapper).registerTypes();
    var payload =
        mapper.writeValueAsString(
            DemandPublishedRequested.builder().id("parent").demandId("demand").build());
    var failure = new IllegalStateException("transaction rolled back");
    Consumer<InfraEvent> handler =
        ignored -> {
          throw failure;
        };

    try (var context = new StaticApplicationContext()) {
      context.getBeanFactory().registerSingleton("demandPublishedRequestedService", handler);
      var dispatcher = new EventDispatcher();
      dispatcher.setApplicationContext(context);

      var thrown =
          assertThrows(
              IllegalStateException.class,
              () -> new EventConsumer(dispatcher, mapper).onMessage(payload));
      assertEquals(failure, thrown);
    }
  }

  @Test
  void malformed_event_is_rejected_without_requeue() {
    var mapper = new JacksonConf().objectMapper();
    new InfraEventTypeRegistrar(mapper).registerTypes();
    var dispatcher = new EventDispatcher();

    assertThrows(
        AmqpRejectAndDontRequeueException.class,
        () -> new EventConsumer(dispatcher, mapper).onMessage("not-json"));
    assertThrows(
        AmqpRejectAndDontRequeueException.class,
        () -> new EventConsumer(dispatcher, mapper).onMessage("null"));
  }

  @Test
  void missing_handler_failure_propagates_for_broker_retry() throws Exception {
    var mapper = new JacksonConf().objectMapper();
    new InfraEventTypeRegistrar(mapper).registerTypes();
    var payload =
        mapper.writeValueAsString(
            DemandPublishedRequested.builder().id("parent").demandId("demand").build());

    try (var context = new StaticApplicationContext()) {
      var dispatcher = new EventDispatcher();
      dispatcher.setApplicationContext(context);

      assertThrows(
          NoSuchBeanDefinitionException.class,
          () -> new EventConsumer(dispatcher, mapper).onMessage(payload));
    }
  }

  @Test
  void routes_historical_parent_with_production_jackson() throws Exception {
    assertRoutes(
        DemandPublishedRequested.builder().id("parent").demandId("demand").build(),
        "demandPublishedRequestedService");
  }

  @Test
  void routes_historical_child_with_production_jackson() throws Exception {
    assertRoutes(
        DemandPublishedNotificationRequested.builder()
            .id("child")
            .demandPublishedRequestedId("parent")
            .sellerId("seller")
            .demandId("demand")
            .build(),
        "demandPublishedNotificationRequestedService");
  }

  @Test
  void routes_offer_using_historical_wire_names() throws Exception {
    assertRoutes(
        DemandPublishedRequested.builder().id("parent").demandId("demand").offerId("offer").build(),
        "demandPublishedRequestedService");
    assertRoutes(
        DemandPublishedNotificationRequested.builder()
            .id("child")
            .demandPublishedRequestedId("parent")
            .demandId("demand")
            .offerId("offer")
            .researcherId("researcher")
            .build(),
        "demandPublishedNotificationRequestedService");
  }

  @Test
  void reads_legacy_payload_without_offer_fields() throws Exception {
    var mapper = new JacksonConf().objectMapper();
    new InfraEventTypeRegistrar(mapper).registerTypes();
    var event =
        (DemandPublishedRequested)
            mapper.readValue(
                "{\"@type\":\"DemandPublishedRequested\",\"id\":\"parent\",\"demand_id\":\"demand\"}",
                InfraEvent.class);
    assertEquals("demand", event.getDemandId());
    org.junit.jupiter.api.Assertions.assertNull(event.getOfferId());
  }

  @SuppressWarnings("unchecked")
  private void assertRoutes(InfraEvent original, String beanName) throws Exception {
    original.setAttemptNb(2);
    var mapper = new JacksonConf().objectMapper();
    new InfraEventTypeRegistrar(mapper).registerTypes();
    var payload = mapper.writeValueAsString(original);
    assertEquals(
        original.getClass().getSimpleName(), mapper.readTree(payload).get("@type").asText());
    Consumer<InfraEvent> handler = mock(Consumer.class);
    try (var context = new StaticApplicationContext()) {
      context.getBeanFactory().registerSingleton(beanName, handler);
      var dispatcher = new EventDispatcher();
      dispatcher.setApplicationContext(context);
      var consumer = new EventConsumer(dispatcher, mapper);
      consumer.onMessage(payload);
      var captor = ArgumentCaptor.forClass(InfraEvent.class);
      verify(handler).accept(captor.capture());
      assertEquals(original.getClass(), captor.getValue().getClass());
      assertEquals(mapper.readTree(payload), mapper.valueToTree(captor.getValue()));
    }
  }
}
