package com.devikapps.vaikaparts.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.devikapps.vaikaparts.config.JacksonConf;
import com.devikapps.vaikaparts.event.config.InfraEventTypeRegistrar;
import com.devikapps.vaikaparts.event.consumer.EventConsumer;
import com.devikapps.vaikaparts.event.consumer.EventDispatcher;
import com.devikapps.vaikaparts.event.model.InfraEvent;
import com.devikapps.vaikaparts.event.model.NotificationBatchRequested;
import com.devikapps.vaikaparts.event.model.NotificationRequested;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationEventRoutingTest {
  @Test
  void routes_renamed_parent_with_production_jackson() throws Exception {
    assertRoutes(
        NotificationBatchRequested.builder().id("parent").demandId("demand").build(),
        "notificationBatchRequestedService");
  }

  @Test
  void routes_renamed_child_with_production_jackson() throws Exception {
    assertRoutes(
        NotificationRequested.builder()
            .id("child")
            .demandPublishedRequestedId("parent")
            .sellerId("seller")
            .demandId("demand")
            .build(),
        "notificationRequestedService");
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
      try {
        consumer.onMessage(payload);
        var captor = ArgumentCaptor.forClass(InfraEvent.class);
        verify(handler, timeout(5000)).accept(captor.capture());
        assertEquals(original.getClass(), captor.getValue().getClass());
        assertEquals(mapper.readTree(payload), mapper.valueToTree(captor.getValue()));
      } finally {
        ((ExecutorService) ReflectionTestUtils.getField(consumer, "executor")).shutdownNow();
      }
    }
  }
}
