package com.devikapps.vaikaparts.repository.model.user;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.ManagerRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "managers")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class JManager extends JUser {
  @Column(name = "manager_role", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private ManagerRole managerRole;

  @Override
  public String toString() {
    return format(
        """
        Manager={
         \tid=%s,
         \tsupabaseUserId=%s,
         \tname=%s,
         \tphoneNumber=%s,
         \tprofileImgUrl=%s,
         \tuserType=%s,
         \tstatus=%s,
         \tcreatedAt=%s,
         \tupdatedAt=%s,
         \tmanagerRole=%s
        }\
        """,
        getId(),
        getSupabaseUserId(),
        getName(),
        getPhoneNumber(),
        getProfileImgUrl(),
        getUserType(),
        getStatus(),
        getCreatedAt(),
        getUpdatedAt(),
        managerRole);
  }
}
