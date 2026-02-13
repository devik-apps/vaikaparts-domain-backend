package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import lombok.Getter;

@Getter
public enum SupabaseEventType {
  USER_CREATED("INSERT"),
  USER_UPDATED("UPDATE"),
  USER_DELETED("DELETE");

  private final String value;

  SupabaseEventType(String value) {
    this.value = value;
  }

  public static SupabaseEventType fromValue(String value) {
    for (SupabaseEventType type : values()) {
      if (type.value.equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException(format("Unknown event type: %s", forJava(value)));
  }
}
