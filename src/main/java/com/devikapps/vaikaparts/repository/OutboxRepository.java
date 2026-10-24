package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.event.JOutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository
    extends JpaRepository<JOutboxEvent, String>, OutboxRepositoryCustom {
  List<JOutboxEvent> findAllByLockTokenOrderByCreatedAt(String lockToken);
}
