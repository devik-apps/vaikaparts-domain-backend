package com.devikapps.vaikaparts.endpoint.rest.controller.util;

import java.util.UUID;

public final class IdempotencyKey {
  private IdempotencyKey() {}

  public static void validate(String value) {
    if (value == null) return;
    UUID parsed;
    try {
      parsed = UUID.fromString(value);
    } catch (IllegalArgumentException error) {
      throw new IllegalArgumentException("Invalid Idempotency-Key");
    }
    if (!parsed.toString().equalsIgnoreCase(value)) {
      throw new IllegalArgumentException("Invalid Idempotency-Key");
    }
  }
}
