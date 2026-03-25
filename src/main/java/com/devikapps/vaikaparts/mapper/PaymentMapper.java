package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.mapper.exchange.OfferMapper;
import com.devikapps.vaikaparts.mapper.user.ResearcherMapper;
import com.devikapps.vaikaparts.model.Payment;
import com.devikapps.vaikaparts.pecunia.client.model.AirtelMoneyPayment;
import com.devikapps.vaikaparts.pecunia.client.model.MvolaPayment;
import com.devikapps.vaikaparts.repository.model.JPayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {OfferMapper.class, ResearcherMapper.class})
public interface PaymentMapper {

  @Mapping(source = "id", target = "paymentId")
  @Mapping(target = "owner", ignore = true)
  @Mapping(target = "relatedOffer", ignore = true)
  @Mapping(source = "payer.phoneNumber", target = "payerMsisdn")
  @Mapping(source = "type", target = "paymentType")
  Payment toDomain(MvolaPayment mvolaPayment);

  @Mapping(source = "id", target = "paymentId")
  @Mapping(target = "owner", ignore = true)
  @Mapping(target = "relatedOffer", ignore = true)
  @Mapping(source = "payer.phoneNumber", target = "payerMsisdn")
  @Mapping(source = "type", target = "paymentType")
  Payment toDomain(AirtelMoneyPayment airtelMoneyPayment);

  @Mapping(source = "paymentId", target = "paymentId")
  Payment toDomain(JPayment jPayment);

  JPayment toPersistence(Payment payment);
}
