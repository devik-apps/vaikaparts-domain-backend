package com.devikapps.vaikaparts.model.user;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.ManagerRole;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public final class Manager extends User {
  private ManagerRole role;

  @Override
  public String toString() {
    return format(
        """
        Manager={
         \tid=%s,
         \tsupabaseUserId=%s,
         \tname=%s,
         \temail=%s,
         \tphoneNumber=%s,
         \tprofileImgUrl=%s,
         \tuserType=%s,
         \tstatus=%s,
         \tcreatedAt=%s,
         \tupdatedAt=%s,
         \trole=%s
        }\
        """,
        getId(),
        getSupabaseUserId(),
        getName(),
        getEmail(),
        getPhoneNumber(),
        getProfileImgUrl(),
        getUserType(),
        getStatus(),
        getCreatedAt(),
        getUpdatedAt(),
        role);
  }
}
