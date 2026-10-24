package com.devikapps.vaikaparts.repository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExternalNotificationDeliveryRepositoryCustom {

  List<String> claimDue(
      LocalDateTime now, LocalDateTime expiredBefore, String lockToken, int batchSize);

  int markSuccess(String id, String lockToken, LocalDateTime deliveredAt);

  int reschedule(
      String id,
      String lockToken,
      int attemptNb,
      LocalDateTime nextAttemptAt,
      String lastError,
      LocalDateTime updatedAt);
}
