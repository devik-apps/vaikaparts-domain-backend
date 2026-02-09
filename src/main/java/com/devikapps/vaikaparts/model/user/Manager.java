package com.devikapps.vaikaparts.model.user;

import com.devikapps.vaikaparts.model.classifier.ManagerRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString
public final class Manager extends User {
  private ManagerRole role;
}
