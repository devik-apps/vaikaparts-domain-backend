package com.devikapps.vaikaparts.model;

import com.devikapps.vaikaparts.pecunia.client.model.PaymentCurrency;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentType;
import java.math.BigDecimal;
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
public class PaymentRequest {
  private Payer payer;
  private BigDecimal amount;
  private PaymentCurrency currency;
  private PaymentType type;
  private String description;
}
