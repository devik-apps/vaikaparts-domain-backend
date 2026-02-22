package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandPublishedRequestedRepository
    extends JpaRepository<JDemandPublishedRequested, String> {

  @Query("SELECT d FROM JDemandPublishedRequested d WHERE d.demand.id = :demandId")
  Optional<JDemandPublishedRequested> findByDemandId(@Param("demandId") String demandId);
}
