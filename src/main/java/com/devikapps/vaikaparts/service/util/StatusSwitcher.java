package com.devikapps.vaikaparts.service.util;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;

import com.devikapps.vaikaparts.model.classifier.PostStatus;

public final class StatusSwitcher {

  private StatusSwitcher() {
    throw new UnsupportedOperationException("UTILITY CLASS");
  }

  public static void updateStatus(StatusAware entity, PostStatus newStatus) {
    final var now = now();

    entity.setStatus(newStatus);
    entity.setUpdatedAt(now);

    switch (newStatus) {
      case CANCELED -> entity.setCanceledAt(now);
      case SUSPENDED -> entity.setSuspendedAt(now);
      case PUBLISHED -> {
        entity.setCanceledAt(null);
        entity.setSuspendedAt(null);
      }
      default ->
          throw new IllegalArgumentException(format("Unsupported PostStatus: %s", newStatus));
    }
  }
}
