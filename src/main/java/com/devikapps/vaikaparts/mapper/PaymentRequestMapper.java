package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.pecunia.client.model.AirtelMoneyPaymentRequest;
import com.devikapps.vaikaparts.pecunia.client.model.BasePaymentRequest;
import com.devikapps.vaikaparts.pecunia.client.model.MvolaPaymentRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentRequestMapper {

  MvolaPaymentRequest toMvola(BasePaymentRequest basePaymentRequest);

  AirtelMoneyPaymentRequest toAirtelMoney(BasePaymentRequest basePaymentRequest);
}
