package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentType;
import com.devikapps.vaikaparts.pecunia.client.model.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
public class Payment {
  private String paymentId;
  private Researcher owner;
  private String payerMsisdn;
  private Offer relatedOffer;
  private VerificationStatus status;
  private PaymentType paymentType;
}
