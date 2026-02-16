package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DemandRepository extends JpaRepository<JDemand, String> {

  @Query(
      """
      SELECT d
      FROM JDemand d
      LEFT JOIN FETCH d.researcher
      LEFT JOIN FETCH d.part
      WHERE d.id = :id
      """)
  Optional<JDemand> findByIdWithRelations(@Param("id") String id);

  @Query(
      value =
          """
          SELECT d
          FROM JDemand d
          LEFT JOIN FETCH d.researcher
          LEFT JOIN FETCH d.part
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          """)
  Page<JDemand> findAllWithRelations(Pageable pageable);

  @Query(
      value =
          """
          SELECT d
          FROM JDemand d
          LEFT JOIN FETCH d.researcher
          LEFT JOIN FETCH d.part
          WHERE d.status = :status
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          WHERE d.status = :status
          """)
  Page<JDemand> findByStatusWithRelations(@Param("status") PostStatus status, Pageable pageable);

  @Query(
      value =
          """
          SELECT d
          FROM JDemand d
          LEFT JOIN FETCH d.researcher
          LEFT JOIN FETCH d.part p
          WHERE p.carBrand = :brand
          AND d.status = 'PUBLISHED'
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          JOIN d.part p
          WHERE p.carBrand = :brand
          AND d.status = 'PUBLISHED'
          """)
  Page<JDemand> findByCarBrand(@Param("brand") String brand, Pageable pageable);

  @Query(
      value =
          """
          SELECT d
          FROM JDemand d
          LEFT JOIN FETCH d.researcher
          LEFT JOIN FETCH d.part p
          WHERE p.partCategory = :category
          AND d.status = 'PUBLISHED'
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          JOIN d.part p
          WHERE p.partCategory = :category
          AND d.status = 'PUBLISHED'
          """)
  Page<JDemand> findByPartCategory(@Param("category") PartCategory category, Pageable pageable);
}
