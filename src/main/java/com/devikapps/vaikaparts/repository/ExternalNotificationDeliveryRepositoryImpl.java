package com.devikapps.vaikaparts.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class ExternalNotificationDeliveryRepositoryImpl
    implements ExternalNotificationDeliveryRepositoryCustom {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public List<String> claimDue(
      LocalDateTime now, LocalDateTime expiredBefore, String lockToken, int batchSize) {
    return jdbcTemplate.queryForList(
        """
        WITH candidates AS (
          SELECT id
          FROM external_notification_deliveries
          WHERE status <> 'SUCCESS'
            AND next_attempt_at <= ?
            AND (locked_at IS NULL OR locked_at < ?)
          ORDER BY next_attempt_at, created_at
          LIMIT ?
          FOR UPDATE SKIP LOCKED
        )
        UPDATE external_notification_deliveries AS delivery
        SET status = 'PROCESSING', locked_at = ?, lock_token = ?, updated_at = ?
        FROM candidates
        WHERE delivery.id = candidates.id
        RETURNING delivery.id
        """,
        String.class,
        Timestamp.valueOf(now),
        Timestamp.valueOf(expiredBefore),
        batchSize,
        Timestamp.valueOf(now),
        lockToken,
        Timestamp.valueOf(now));
  }

  @Override
  public int markSuccess(String id, String lockToken, LocalDateTime deliveredAt) {
    return jdbcTemplate.update(
        """
        UPDATE external_notification_deliveries
        SET status = 'SUCCESS', delivered_at = ?, last_error = NULL,
            updated_at = ?, locked_at = NULL, lock_token = NULL
        WHERE id = ? AND lock_token = ?
        """,
        Timestamp.valueOf(deliveredAt),
        Timestamp.valueOf(deliveredAt),
        id,
        lockToken);
  }

  @Override
  public int reschedule(
      String id,
      String lockToken,
      int attemptNb,
      LocalDateTime nextAttemptAt,
      String lastError,
      LocalDateTime updatedAt) {
    return jdbcTemplate.update(
        """
        UPDATE external_notification_deliveries
        SET status = 'FAILED', attempt_nb = ?, next_attempt_at = ?, last_error = ?,
            updated_at = ?, locked_at = NULL, lock_token = NULL
        WHERE id = ? AND lock_token = ?
        """,
        attemptNb,
        Timestamp.valueOf(nextAttemptAt),
        lastError,
        Timestamp.valueOf(updatedAt),
        id,
        lockToken);
  }
}
