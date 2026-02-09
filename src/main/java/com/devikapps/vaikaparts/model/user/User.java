package com.devikapps.vaikaparts.model.user;

import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public abstract sealed class User permits Researcher, Seller, Manager {
  private String id;
  private String supabaseUserId;
  private String name;
  private String phoneNumber;
  private String profileImgUrl;
  private UserType userType;
  private UserStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
