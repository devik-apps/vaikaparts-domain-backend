package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<JUser, String> {
  Optional<JUser> findBySupabaseUserId(String supabaseUserId);

  @Query(value = "SELECT pg_advisory_xact_lock(CAST(hashtext(:key) AS bigint))", nativeQuery = true)
  Object lockByKey(@Param("key") String key);

  Optional<JUser> findJUserById(String id);

  boolean existsBySupabaseUserId(String supabaseUserId);

  Page<JUser> findAllByUserType(UserType userType, Pageable pageable);

  List<JUser> findAllByUserTypeAndStatus(UserType userType, UserStatus userStatus);

}
