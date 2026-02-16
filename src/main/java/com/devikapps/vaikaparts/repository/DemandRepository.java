package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

  Page<JDemand> findByStatus(PostStatus status, Pageable pageable);

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

  Page<JDemand> findByResearcherId(String researcherId, Pageable pageable);

  @Query(
      value =
          """
          SELECT d
          FROM JDemand d
          LEFT JOIN FETCH d.researcher
          LEFT JOIN FETCH d.part
          WHERE d.researcher.id = :researcherId
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          WHERE d.researcher.id = :researcherId
          """)
  Page<JDemand> findByResearcherIdWithRelations(
      @Param("researcherId") String researcherId, Pageable pageable);

  @Query(
      value =
          """
          SELECT d
          FROM JDemand d
          LEFT JOIN FETCH d.researcher
          LEFT JOIN FETCH d.part
          WHERE d.researcher.id = :researcherId
          AND d.status = :status
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          WHERE d.researcher.id = :researcherId
          AND d.status = :status
          """)
  Page<JDemand> findByResearcherIdAndStatus(
      @Param("researcherId") String researcherId,
      @Param("status") PostStatus status,
      Pageable pageable);

  long countByResearcherId(String researcherId);

  long countByStatus(PostStatus status);

  long countByResearcherIdAndStatus(String researcherId, PostStatus status);

  boolean existsByIdAndResearcherId(String id, String researcherId);

  boolean existsByResearcherIdAndStatus(String researcherId, PostStatus status);

  @Query(
      """
      SELECT d FROM JDemand d
      WHERE d.createdAt BETWEEN :startDate AND :endDate
      """)
  Page<JDemand> findByCreatedAtBetween(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);

  @Query(
      value =
          """
          SELECT d
          FROM JDemand d
          LEFT JOIN FETCH d.researcher
          LEFT JOIN FETCH d.part
          WHERE d.createdAt >= :since
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          WHERE d.createdAt >= :since
          """)
  Page<JDemand> findRecentDemandsWithRelations(
      @Param("since") LocalDateTime since, Pageable pageable);

  @Query(
      value =
          """
          SELECT d
          FROM JDemand d
          LEFT JOIN FETCH d.researcher
          LEFT JOIN FETCH d.part
          WHERE d.status = 'PUBLISHED'
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          WHERE d.status = 'PUBLISHED'
          """)
  Page<JDemand> findAllPublishedWithRelations(Pageable pageable);

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
          WHERE p.carBrand = :brand
          AND p.carModel = :model
          AND d.status = 'PUBLISHED'
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          JOIN d.part p
          WHERE p.carBrand = :brand
          AND p.carModel = :model
          AND d.status = 'PUBLISHED'
          """)
  Page<JDemand> findByCarBrandAndModel(
      @Param("brand") String brand, @Param("model") String model, Pageable pageable);

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

  @Query(
      value =
          """
          SELECT d
          FROM JDemand d
          LEFT JOIN FETCH d.researcher
          LEFT JOIN FETCH d.part p
          WHERE p.carBrand = :brand
          AND p.carModel = :model
          AND p.carYear = :year
          AND d.status = 'PUBLISHED'
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          JOIN d.part p
          WHERE p.carBrand = :brand
          AND p.carModel = :model
          AND p.carYear = :year
          AND d.status = 'PUBLISHED'
          """)
  Page<JDemand> findByCarBrandModelAndYear(
      @Param("brand") String brand,
      @Param("model") String model,
      @Param("year") int year,
      Pageable pageable);

  @Query(
      value =
          """
          SELECT d
          FROM JDemand d
          LEFT JOIN FETCH d.researcher
          LEFT JOIN FETCH d.part
          WHERE LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
          AND d.status = 'PUBLISHED'
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          WHERE LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
          AND d.status = 'PUBLISHED'
          """)
  Page<JDemand> searchByDescriptionKeyword(@Param("keyword") String keyword, Pageable pageable);

  @Modifying
  @Query("UPDATE JDemand d SET d.status = :status, d.updatedAt = :updatedAt WHERE d.id = :id")
  int updateStatus(
      @Param("id") String id,
      @Param("status") PostStatus status,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Modifying
  @Query(
      """
      UPDATE JDemand d
      SET d.status = :newStatus, d.updatedAt = :updatedAt
      WHERE d.status = :oldStatus
      """)
  int updateStatusForAll(
      @Param("oldStatus") PostStatus oldStatus,
      @Param("newStatus") PostStatus newStatus,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Modifying
  @Query(
      """
      UPDATE JDemand d
      SET d.canceledAt = :canceledAt, d.status = 'CANCELED', d.updatedAt = :updatedAt
      WHERE d.id = :id
      """)
  int cancelDemand(
      @Param("id") String id,
      @Param("canceledAt") LocalDateTime canceledAt,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Modifying
  @Query(
      """
      UPDATE JDemand d
      SET d.suspendedAt = :suspendedAt, d.status = 'SUSPENDED', d.updatedAt = :updatedAt
      WHERE d.id = :id
      """)
  int suspendDemand(
      @Param("id") String id,
      @Param("suspendedAt") LocalDateTime suspendedAt,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Query(
      value =
          """
          SELECT d
          FROM JDemand d
          LEFT JOIN FETCH d.researcher
          LEFT JOIN FETCH d.part
          WHERE d.canceledAt IS NULL
          AND d.suspendedAt IS NULL
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          WHERE d.canceledAt IS NULL
          AND d.suspendedAt IS NULL
          """)
  Page<JDemand> findAllActiveDemands(Pageable pageable);

  @Query(
      value =
          """
          SELECT d
          FROM JDemand d
          LEFT JOIN FETCH d.researcher
          LEFT JOIN FETCH d.part
          WHERE d.researcher.id = :researcherId
          AND d.canceledAt IS NULL
          AND d.suspendedAt IS NULL
          """,
      countQuery =
          """
          SELECT COUNT(d.id)
          FROM JDemand d
          WHERE d.researcher.id = :researcherId
          AND d.canceledAt IS NULL
          AND d.suspendedAt IS NULL
          """)
  Page<JDemand> findActiveDemandsForResearcher(
      @Param("researcherId") String researcherId, Pageable pageable);
}
