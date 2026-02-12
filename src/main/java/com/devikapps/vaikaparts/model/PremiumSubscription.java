package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.model.classifier.PremiumPlan;
import com.devikapps.vaikaparts.model.user.Seller;
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
@EqualsAndHashCode
@ToString
public class PremiumSubscription {
  private String id;
  private Seller seller;
  private PremiumPlan plan;
}
