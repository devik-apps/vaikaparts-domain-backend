package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.event.JNotification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<JNotification, String> {

  @Query("SELECT n FROM JNotification n WHERE n.seller.id = :sellerId ORDER BY n.createdAt DESC")
  Page<JNotification> findBySellerIdOrderByCreatedAtDesc(
      @Param("sellerId") String sellerId, Pageable pageable);

  @Query(
      "SELECT n FROM JNotification n WHERE n.seller.id = :sellerId AND n.read = :isRead ORDER BY"
          + " n.createdAt DESC")
  Page<JNotification> findBySellerIdAndIsReadOrderByCreatedAtDesc(
      @Param("sellerId") String sellerId, @Param("isRead") boolean isRead, Pageable pageable);

  @Query("SELECT COUNT(n) FROM JNotification n WHERE n.seller.id = :sellerId AND n.read = :isRead")
  long countBySellerIdAndIsRead(
      @Param("sellerId") String sellerId, @Param("isRead") boolean isRead);

  @Query("SELECT n FROM JNotification n WHERE n.id = :notificationId AND n.seller.id = :sellerId")
  Optional<JNotification> findByIdAndSellerId(
      @Param("notificationId") String notificationId, @Param("sellerId") String sellerId);
}
