package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandPublishedNotificationRepository
    extends JpaRepository<JDemandPublishedNotification, String> {

  @Query(
      "SELECT n FROM JDemandPublishedNotification n WHERE n.recipient.id = :recipientId ORDER BY"
          + " n.createdAt DESC")
  Page<JDemandPublishedNotification> findByRecipientIdOrderByCreatedAtDesc(
      @Param("recipientId") String recipientId, Pageable pageable);

  @Query(
      "SELECT n FROM JDemandPublishedNotification n WHERE n.recipient.id = :recipientId AND n.read"
          + " = :isRead ORDER BY n.createdAt DESC")
  Page<JDemandPublishedNotification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(
      @Param("recipientId") String recipientId, @Param("isRead") boolean isRead, Pageable pageable);

  @Query(
      "SELECT COUNT(n) FROM JDemandPublishedNotification n WHERE n.recipient.id = :recipientId AND"
          + " n.read = :isRead")
  long countByRecipientIdAndIsRead(
      @Param("recipientId") String recipientId, @Param("isRead") boolean isRead);

  @Query(
      "SELECT n FROM JDemandPublishedNotification n WHERE n.id = :notificationId AND n.recipient.id"
          + " = :recipientId")
  Optional<JDemandPublishedNotification> findByIdAndRecipientId(
      @Param("notificationId") String notificationId, @Param("recipientId") String recipientId);
}
