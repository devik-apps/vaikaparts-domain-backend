package com.devikapps.vaikaparts.repository.model.user;

import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
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
@ToString
@SuperBuilder(toBuilder = true)
public class JUser {
  @Id private String id;

  @Column(name = "supabase_user_id", nullable = false)
  private String supabaseUserId;

  private String name;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(name = "profile_img_url")
  private String profileImgUrl;

  @Column(name = "user_type")
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private UserType userType;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private UserStatus status;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
