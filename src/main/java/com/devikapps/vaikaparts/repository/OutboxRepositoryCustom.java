package com.devikapps.vaikaparts.repository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepositoryCustom {
  List<String> claimDue(
      LocalDateTime now, LocalDateTime expiredBefore, String lockToken, int batchSize);

  int markPublished(String id, String lockToken, LocalDateTime publishedAt);

  int reschedule(
      String id, String lockToken, int attemptNb, LocalDateTime nextAttemptAt, String lastError);
}
