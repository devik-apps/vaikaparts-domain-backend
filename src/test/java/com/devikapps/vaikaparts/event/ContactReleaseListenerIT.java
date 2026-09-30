package com.devikapps.vaikaparts.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;

import com.devikapps.vaikaparts.event.consumer.ContactReleaseListener;
import com.devikapps.vaikaparts.event.model.ContactReleaseRequested;
import com.devikapps.vaikaparts.service.ContactReleaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.TransientDataAccessResourceException;

@ExtendWith(MockitoExtension.class)
class ContactReleaseListenerIT {

  @Mock private ContactReleaseService service;
  private ContactReleaseListener listener;
  private ContactReleaseRequested event;

  @BeforeEach
  void setUp() throws Exception {
    var mapper = new ObjectMapper().findAndRegisterModules();
    listener = new ContactReleaseListener(service, mapper);
    event =
        ContactReleaseRequested.builder()
            .id("event-id")
            .paymentId("payment-id")
            .unlockRequestId("unlock-id")
            .buyerId("buyer-id")
            .sellerId("seller-id")
            .provider("VANILLA_PAY")
            .paidAt(Instant.parse("2026-09-30T10:15:30Z"))
            .build();
  }

  @Test
  void shouldRejectPermanentInvalidEventsWithoutRequeue() throws Exception {
    doThrow(new IllegalArgumentException("mismatch"))
        .when(service)
        .release(any(ContactReleaseRequested.class));

    assertThrows(
        AmqpRejectAndDontRequeueException.class,
        () -> listener.onMessage(new ObjectMapper().findAndRegisterModules().writeValueAsString(event)));
  }

  @Test
  void shouldPropagateTransientDatabaseFailuresForRetry() throws Exception {
    var failure = new TransientDataAccessResourceException("database unavailable");
    doThrow(failure).when(service).release(any(ContactReleaseRequested.class));

    assertThrows(
        TransientDataAccessResourceException.class,
        () -> listener.onMessage(new ObjectMapper().findAndRegisterModules().writeValueAsString(event)));
  }
}
