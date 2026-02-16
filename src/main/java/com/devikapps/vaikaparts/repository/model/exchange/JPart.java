package com.devikapps.vaikaparts.repository.model.exchange;

import com.devikapps.vaikaparts.model.classifier.PartCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "car_parts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@SuperBuilder
public class JPart {
  @Id private String id;

  @Column(name = "part_name")
  private String partName;

  @Column(name = "car_brand")
  private String carBrand;

  @Column(name = "car_model")
  private String carModel;

  @Column(name = "car_year")
  private int carYear;

  @Column(name = "image_bucket")
  private String imageBucket;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "part_category")
  private PartCategory partCategory;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "demand_id", nullable = false, unique = true)
  private JDemand demand;
}
