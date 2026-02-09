package com.devikapps.vaikaparts.repository.model.user;

import com.devikapps.vaikaparts.repository.model.JLatLon;
import com.devikapps.vaikaparts.repository.model.JLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sellers")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class JSeller extends JUser {
  @Column(name = "garage_name")
  private String garageName;

  @Embedded private JLocation location;

  @Embedded private JLatLon latLon;
}
