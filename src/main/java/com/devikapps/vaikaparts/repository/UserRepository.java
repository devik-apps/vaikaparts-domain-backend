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

  Optional<JUser> findJUserById(String id);

  boolean existsBySupabaseUserId(String supabaseUserId);

  Page<JUser> findAllByUserType(UserType userType, Pageable pageable);

  List<JUser> findAllByUserTypeAndStatus(UserType userType, UserStatus userStatus);


  @Modifying
  @Transactional
  @Query(value = """
    INSERT INTO users (id, supabase_user_id, name, email, phone_number,
                       user_type, status, profile_img_key, created_at, updated_at)
    VALUES (:#{#user.id}, :#{#user.supabaseUserId}, :#{#user.name}, 
            :#{#user.email}, :#{#user.phoneNumber}, :#{#user.userType},
            :#{#user.status}, :#{#user.profileImgKey},
            :#{#user.createdAt}, :#{#user.updatedAt})
    ON CONFLICT (supabase_user_id) DO NOTHING
    """, nativeQuery = true)
  void insertIfAbsent(@Param("user") JUser user);
}
