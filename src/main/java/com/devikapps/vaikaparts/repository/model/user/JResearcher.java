package com.devikapps.vaikaparts.repository.model.user;

import static java.lang.String.format;

import com.devikapps.vaikaparts.repository.model.JLocation;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "researchers")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class JResearcher extends JUser {

  @Embedded private JLocation location;

  @Override
  public String toString() {
    return format(
        """
        Researcher={
         \tid=%s,
         \tsupabaseUserId=%s,
         \tname=%s,
         \tphoneNumber=%s,
         \tprofileImgUrl=%s,
         \tuserType=%s,
         \tstatus=%s,
         \tcreatedAt=%s,
         \tupdatedAt=%s,
         \tlocation=%s
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
        location);
  }
}
