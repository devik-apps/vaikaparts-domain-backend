package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus;
import com.devikapps.vaikaparts.repository.model.exchange.JContactUnlock;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactUnlockRepository extends JpaRepository<JContactUnlock, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT unlock FROM JContactUnlock unlock WHERE unlock.unlockRequestId = :unlockRequestId")
  Optional<JContactUnlock> findByUnlockRequestIdForUpdate(
      @Param("unlockRequestId") String unlockRequestId);

  Optional<JContactUnlock> findByBuyerIdAndOfferIdAndStatusIn(
      String buyerId, String offerId, Collection<ContactUnlockStatus> statuses);

  List<JContactUnlock> findByProviderAndStatusOrderByCreatedAtAsc(
      String provider, ContactUnlockStatus status);
}
