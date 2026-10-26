package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.model.notification.JWebPushSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebPushSubscriptionRepository extends JpaRepository<JWebPushSubscription, String> {
  Optional<JWebPushSubscription> findByEndpoint(String endpoint);

  List<JWebPushSubscription> findAllByUserId(String userId);

  @Modifying
  @Query("delete from JWebPushSubscription s where s.endpoint = :endpoint and s.user.id = :userId")
  void deleteByEndpointAndUserId(
      @Param("endpoint") String endpoint, @Param("userId") String userId);
}
