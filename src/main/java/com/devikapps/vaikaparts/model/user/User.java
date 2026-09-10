package com.devikapps.vaikaparts.model.user;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URL;
import java.time.OffsetDateTime;
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
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "userType",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = Researcher.class, name = "RESEARCHER"),
  @JsonSubTypes.Type(value = Seller.class, name = "SELLER"),
  @JsonSubTypes.Type(value = Manager.class, name = "MANAGER")
})
public abstract sealed class User permits Researcher, Seller, Manager {
  private String id;
  private String supabaseUserId;
  private String name;
  private String email;
  private String phoneNumber;
  private URL profileImgUrl;
  private UserType userType;
  private UserStatus status;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

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
