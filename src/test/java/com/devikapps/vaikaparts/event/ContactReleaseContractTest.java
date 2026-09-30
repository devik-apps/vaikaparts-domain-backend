package com.devikapps.vaikaparts.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.devikapps.vaikaparts.config.ContactReleaseRabbitConf;
import com.devikapps.vaikaparts.config.JacksonConf;
import com.devikapps.vaikaparts.event.model.ContactReleaseRequested;
import com.devikapps.vaikaparts.event.model.InfraEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

class ContactReleaseContractTest {

  private static final Path FIXTURE =
      Path.of("src/test/resources/contracts/contact-release-requested-v1.json");

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new JacksonConf().objectMapper();
    objectMapper.registerSubtypes(
        new NamedType(ContactReleaseRequested.class, "ContactReleaseRequested"));
  }

  @Test
  void shouldDeserializeAndSerializeTheSharedContract() throws Exception {
    var expectedJson = objectMapper.readTree(Files.readString(FIXTURE));

    var event = objectMapper.readValue(Files.readString(FIXTURE), InfraEvent.class);

    assertTrue(event instanceof ContactReleaseRequested);
    var release = (ContactReleaseRequested) event;
    assertEquals("event-id", release.getId());
    assertEquals("payment-id", release.getPaymentId());
    assertEquals("unlock-id", release.getUnlockRequestId());
    assertEquals("buyer-id", release.getBuyerId());
    assertEquals("seller-id", release.getSellerId());
    assertEquals("VANILLA_PAY", release.getProvider());
    assertEquals(Instant.parse("2026-09-30T10:15:30Z"), release.getPaidAt());
    assertEquals(0, release.getAttemptNb());

    var serialized = objectMapper.readTree(objectMapper.writeValueAsString(release));
    assertEquals(expectedJson, serialized);
    assertFalse(serialized.has("event_source"));
  }

  @Test
  void shouldMatchThePecuniaContractFixtureWhenAvailable() throws Exception {
    var pecuniaFixture =
        Path.of(
            "/home/kyle/projects/devikapps/vaikaparts-pecunia/src/test/resources/contracts/contact-release-requested-v1.json");
    if (Files.exists(pecuniaFixture)) {
      assertEquals(objectMapper.readTree(FIXTURE.toFile()), objectMapper.readTree(pecuniaFixture.toFile()));
    }
  }

  @Test
  void shouldDeclareDedicatedDurableQueueAndRouting() {
    var conf = new ContactReleaseRabbitConf();

    Queue queue = conf.contactReleaseQueue();
    Queue deadLetterQueue = conf.contactReleaseDeadLetterQueue();
    Binding binding = conf.contactReleaseBinding(queue, conf.contactReleaseExchange());

    assertEquals("domain.contact-release.requested", queue.getName());
    assertTrue(queue.isDurable());
    assertEquals("vaikaparts.events", binding.getExchange());
    assertEquals("payment.contact-release.requested", binding.getRoutingKey());
    assertEquals("vaikaparts.events.dlx", queue.getArguments().get("x-dead-letter-exchange"));
    assertEquals(
        "payment.contact-release.requested.dead",
        queue.getArguments().get("x-dead-letter-routing-key"));
    assertEquals("domain.contact-release.requested.dlq", deadLetterQueue.getName());
    assertTrue(deadLetterQueue.isDurable());
    assertNotNull(
        conf.contactReleaseListenerContainerFactory(mock(ConnectionFactory.class)));
  }
}
