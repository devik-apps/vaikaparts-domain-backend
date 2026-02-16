package com.devikapps.vaikaparts.repository.model.exchange;

import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PartCondition;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "offer_part_infos")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@SuperBuilder
public class JPartInfo {
  @Id private String id;

  @Column(name = "part_name")
  private String partName;

  @Column(name = "car_brand")
  private String carBrand;

  @Column(name = "car_model")
  private String carModel;

  @Column(name = "car_year")
  private int carYear;

  @ElementCollection
  @CollectionTable(name = "part_info_photos", joinColumns = @JoinColumn(name = "part_id"))
  @Column(name = "part_image_bucket")
  private List<String> partImageBuckets;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "part_category")
  private PartCategory partCategory;

  // PartInfo specific fields
  @Column(name = "description")
  private String description;

  @Column(name = "price")
  private double price;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "condition")
  private PartCondition condition;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "offer_id", nullable = false, unique = true)
  private JOffer offer;
}
