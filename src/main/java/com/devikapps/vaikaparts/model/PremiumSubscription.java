package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.model.classifier.PremiumPlan;
import com.devikapps.vaikaparts.model.user.Seller;
import lombok.*;

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
