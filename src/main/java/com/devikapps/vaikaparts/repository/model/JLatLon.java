package com.devikapps.vaikaparts.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JLatLon {
  @Column(name = "latitude")
  private double latitude;

  @Column(name = "longitude")
  private double longitude;
}
