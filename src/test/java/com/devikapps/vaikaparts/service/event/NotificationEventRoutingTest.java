package com.devikapps.vaikaparts.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.devikapps.vaikaparts.config.JacksonConf;
import com.devikapps.vaikaparts.event.config.InfraEventTypeRegistrar;
import com.devikapps.vaikaparts.event.consumer.EventConsumer;
import com.devikapps.vaikaparts.event.consumer.EventDispatcher;
import com.devikapps.vaikaparts.event.model.DemandPublishedNotificationRequested;
import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.event.model.InfraEvent;
import com.rabbitmq.client.Channel;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationEventRoutingTest {
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
    Channel channel = mock(Channel.class);
    long deliveryTag = 1L;
    try (var context = new StaticApplicationContext()) {
      context.getBeanFactory().registerSingleton(beanName, handler);
      var dispatcher = new EventDispatcher();
      dispatcher.setApplicationContext(context);
      var consumer = new EventConsumer(dispatcher, mapper);
      try {
        consumer.onMessage(payload, channel, deliveryTag);
        var captor = ArgumentCaptor.forClass(InfraEvent.class);
        verify(handler, timeout(5000)).accept(captor.capture());
        assertEquals(original.getClass(), captor.getValue().getClass());
        assertEquals(mapper.readTree(payload), mapper.valueToTree(captor.getValue()));
        verify(channel).basicAck(deliveryTag, false);
      } finally {
        ((ExecutorService)
                Objects.requireNonNull(ReflectionTestUtils.getField(consumer, "executor")))
            .shutdownNow();
      }
    }
  }
}
