package com.devikapps.vaikaparts.model.user;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import java.net.URL;
import java.time.LocalDateTime;
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
@EqualsAndHashCode
@SuperBuilder
public abstract sealed class User permits Researcher, Seller, Manager {
  private String id;
  private String supabaseUserId;
  private String name;
  private String email;
  private String phoneNumber;
  private URL profileImgUrl;
  private UserType userType;
  private UserStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Override
  public String toString() {
    return format(
        """
        User={
        \tid=%s,
        \tsupabaseUserId=%s,
        \tname=%s,
        \temail=%s,
        \tphoneNumber=%s,
        \tprofileImgUrl=%s,
        \tuserType=%s,
        \tstatus=%s,
        \tcreatedAt=%s,
        \tupdatedAt=%s
        }\
        """,
        id,
        supabaseUserId,
        name,
        email,
        phoneNumber,
        profileImgUrl,
        userType,
        status,
        createdAt,
        updatedAt);
  }
}
