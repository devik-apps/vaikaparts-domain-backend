package com.devikapps.vaikaparts.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class OutboxRepositoryImpl implements OutboxRepositoryCustom {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public List<String> claimDue(
      LocalDateTime now, LocalDateTime expiredBefore, String lockToken, int batchSize) {
    return jdbcTemplate.queryForList(
        """
        WITH candidates AS (
          SELECT id
          FROM event_outbox
          WHERE published_at IS NULL
            AND next_attempt_at <= ?
            AND (locked_at IS NULL OR locked_at < ?)
          ORDER BY created_at
          LIMIT ?
          FOR UPDATE SKIP LOCKED
        )
        UPDATE event_outbox AS outbox
        SET locked_at = ?, lock_token = ?
        FROM candidates
        WHERE outbox.id = candidates.id
        RETURNING outbox.id
        """,
        String.class,
        Timestamp.valueOf(now),
        Timestamp.valueOf(expiredBefore),
        batchSize,
        Timestamp.valueOf(now),
        lockToken);
  }

  @Override
  public int markPublished(String id, String lockToken, LocalDateTime publishedAt) {
    return jdbcTemplate.update(
        """
        UPDATE event_outbox
        SET published_at = ?, last_error = NULL, locked_at = NULL, lock_token = NULL
        WHERE id = ? AND lock_token = ? AND published_at IS NULL
        """,
        Timestamp.valueOf(publishedAt),
        id,
        lockToken);
  }

  @Override
  public int reschedule(
      String id, String lockToken, int attemptNb, LocalDateTime nextAttemptAt, String lastError) {
    return jdbcTemplate.update(
        """
        UPDATE event_outbox
        SET attempt_nb = ?, next_attempt_at = ?, last_error = ?, locked_at = NULL, lock_token = NULL
        WHERE id = ? AND lock_token = ? AND published_at IS NULL
        """,
        attemptNb,
        Timestamp.valueOf(nextAttemptAt),
        lastError,
        id,
        lockToken);
  }
}
