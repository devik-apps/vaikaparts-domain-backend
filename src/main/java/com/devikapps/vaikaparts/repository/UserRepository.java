package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<JUser, String> {
  Optional<JUser> findBySupabaseUserId(String supabaseUserId);

  Optional<JUser> findJUserById(String id);

  boolean existsBySupabaseUserId(String supabaseUserId);

  Page<JUser> findAllByUserType(UserType userType, Pageable pageable);
}
