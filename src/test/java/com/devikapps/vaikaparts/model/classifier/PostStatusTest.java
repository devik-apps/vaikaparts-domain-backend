package com.devikapps.vaikaparts.model.classifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PostStatusTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void should_serialize_satisfied_as_its_public_api_value() throws Exception {
    assertEquals("\"SATISFIED\"", objectMapper.writeValueAsString(PostStatus.SATISFIED));
  }

  @Test
  void should_deserialize_satisfied_from_its_public_api_value() throws Exception {
    assertEquals(PostStatus.SATISFIED, objectMapper.readValue("\"SATISFIED\"", PostStatus.class));
  }
}
