package com.devikapps.vaikaparts.repository.model.user;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
public class JUser {
  @Id private String id;

  @Column(name = "supabase_user_id", nullable = false)
  private String supabaseUserId;

  private String name;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(name = "email")
  private String email;

  @Column(name = "profile_img_key")
  private String profileImgKey;

  @Column(name = "user_type", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private UserType userType;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private UserStatus status;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Override
  public String toString() {
    return format(
        """
        JUser={
        \tid=%s,
        \tsupabaseUserId=%s,
        \tname=%s,
        \tphoneNumber=%s,
        \tprofileImgKey=%s,
        \tuserType=%s,
        \tstatus=%s,
        \tcreatedAt=%s,
        \tupdatedAt=%s
        }\
        """,
        id,
        supabaseUserId,
        name,
        phoneNumber,
        profileImgKey,
        userType,
        status,
        createdAt,
        updatedAt);
  }
}
