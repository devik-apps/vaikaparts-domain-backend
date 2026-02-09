package com.devikapps.vaikaparts.repository.model;

import com.devikapps.vaikaparts.model.classifier.City;
import com.devikapps.vaikaparts.model.classifier.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JLocation {
  @Column(name = "city")
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private City city;

  @Column(name = "region")
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private Region region;

  @Column(name = "address")
  private String address;
}
