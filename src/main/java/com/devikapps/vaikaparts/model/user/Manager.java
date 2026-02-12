package com.devikapps.vaikaparts.model.user;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.ManagerRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
        getPhoneNumber(),
        getProfileImgUrl(),
        getUserType(),
        getStatus(),
        getCreatedAt(),
        getUpdatedAt(),
        role);
  }
}
