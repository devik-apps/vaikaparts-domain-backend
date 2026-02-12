package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.model.user.JUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<JUser, String> {
  Optional<JUser> findBySupabaseUserId(String supabaseUserId);

  boolean existsBySupabaseUserId(String supabaseUserId);
}
