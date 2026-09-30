package com.devikapps.vaikaparts.event.consumer;

import static com.devikapps.vaikaparts.config.ContactReleaseRabbitConf.QUEUE;

import com.devikapps.vaikaparts.event.model.ContactReleaseRequested;
import com.devikapps.vaikaparts.service.ContactReleaseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContactReleaseListener {

  private final ContactReleaseService service;
  private final ObjectMapper objectMapper;

  @RabbitListener(
      queues = QUEUE,
      containerFactory = "contactReleaseListenerContainerFactory")
  public void onMessage(String rawMessage) {
    final ContactReleaseRequested event;
    try {
      event = objectMapper.readValue(rawMessage, ContactReleaseRequested.class);
    } catch (JsonProcessingException e) {
      throw new AmqpRejectAndDontRequeueException("Malformed contact release event", e);
    }

    try {
      service.release(event);
    } catch (IllegalArgumentException e) {
      throw new AmqpRejectAndDontRequeueException("Invalid contact release event", e);
    }
  }
}
