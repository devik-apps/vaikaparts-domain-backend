package com.devikapps.vaikaparts.repository.model;

import static java.lang.String.format;

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
  @Column(name = "city", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private City city;

  @Column(name = "region", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private Region region;

  @Column(name = "address", nullable = false)
  private String address;

  @Override
  public String toString() {
    return format("{ city=%s, region=%s, address=%s }", city, region, address);
  }
}
