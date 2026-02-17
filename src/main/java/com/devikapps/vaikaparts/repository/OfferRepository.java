package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferRepository extends JpaRepository<JOffer, String> {

  @Query(
      """
      SELECT o FROM JOffer o
      LEFT JOIN FETCH o.seller
      LEFT JOIN FETCH o.demand
      LEFT JOIN FETCH o.partInfo pi
      LEFT JOIN FETCH pi.partImageBuckets
      WHERE o.id = :offerId
      """)
  Optional<JOffer> findByIdWithRelations(@Param("offerId") String offerId);

  @Query(
      """
      SELECT o FROM JOffer o
      LEFT JOIN FETCH o.seller
      LEFT JOIN FETCH o.demand
      LEFT JOIN FETCH o.partInfo pi
      LEFT JOIN FETCH pi.partImageBuckets
      WHERE o.seller.id = :sellerId AND o.status = :status
      """)
  Page<JOffer> findBySellerIdAndStatus(
      @Param("sellerId") String sellerId, @Param("status") PostStatus status, Pageable pageable);

  @Query(
      """
      SELECT o FROM JOffer o
      LEFT JOIN FETCH o.seller
      LEFT JOIN FETCH o.demand
      LEFT JOIN FETCH o.partInfo pi
      LEFT JOIN FETCH pi.partImageBuckets
      WHERE o.seller.id = :sellerId
      """)
  Page<JOffer> findBySellerIdWithRelations(@Param("sellerId") String sellerId, Pageable pageable);

  @Query(
      """
      SELECT o FROM JOffer o
      LEFT JOIN FETCH o.seller
      LEFT JOIN FETCH o.partInfo pi
      LEFT JOIN FETCH pi.partImageBuckets
      WHERE o.demand.id = :demandId
      """)
  Page<JOffer> findByDemandId(@Param("demandId") String demandId, Pageable pageable);

  boolean existsByDemandIdAndSellerId(String demandId, String sellerId);
}
