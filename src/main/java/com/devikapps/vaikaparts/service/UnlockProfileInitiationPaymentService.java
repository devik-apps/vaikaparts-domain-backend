package com.devikapps.vaikaparts.service;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.mapper.PaymentMapper;
import com.devikapps.vaikaparts.mapper.PaymentRequestMapper;
import com.devikapps.vaikaparts.model.Payer;
import com.devikapps.vaikaparts.model.Payment;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.pecunia.client.api.AirtelMoneyApi;
import com.devikapps.vaikaparts.pecunia.client.api.MVolaApi;
import com.devikapps.vaikaparts.pecunia.client.model.BasePaymentRequest;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentCurrency;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentParty;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentType;
import com.devikapps.vaikaparts.repository.PaymentRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@Validated
@RequiredArgsConstructor
public class UnlockProfileInitiationPaymentService {

  private static final String DESCRIPTION_TEMPLATE = "VaikaParts App Profile Unlock";

  private final MVolaApi mVolaApi;
  private final AirtelMoneyApi airtelMoneyApi;
  private final PaymentRequestMapper paymentRequestMapper;
  private final PaymentMapper paymentMapper;
  private final PaymentRepository paymentRepository;

  @Value("${vaikaparts.payment.policy.unlock-profile}")
  private String amountOfUnlockProfile;

  public Payment initiateAirtelMoneyPaymentForSellerProfileUnlock(
      @NotNull @NotBlank String sellerId,
      @NotNull Payer payer,
      @NotNull Offer relatedOffer,
      @NotNull Researcher owner) {
    log.info(
        "Request unlock seller profile by Airtel Money Payment for sellerId={} by payer={}",
        forJava(sellerId),
        forJava(payer.phoneNumber()));
    var request = paymentRequestMapper.toAirtelMoney(buildPaymentRequest(payer));

    var paymentReqRes = airtelMoneyApi.initiateAirtelMoneyPayment(request);
    log.info("Airtel Money payment initiated with id={}", paymentReqRes.getId());

    var payment = paymentMapper.toDomain(paymentReqRes);
    payment.setRelatedOffer(relatedOffer);
    payment.setOwner(owner);

    paymentRepository.save(paymentMapper.toPersistence(payment));

    return payment;
  }

  public Payment initiateMvolaPaymentForSellerProfileUnlock(
      @NotNull @NotBlank String sellerId,
      @NotNull Payer payer,
      @NotNull Offer relatedOffer,
      @NotNull Researcher owner) {
    log.info(
        "Request unlock seller profile by Mvola Payment for sellerId={} by payer={}",
        forJava(sellerId),
        forJava(payer.phoneNumber()));
    var request = paymentRequestMapper.toMvola(buildPaymentRequest(payer));

    var paymentReqRes = mVolaApi.initiateMvolaPayment(request);
    log.info("Mvola Payment initiated with id={}", paymentReqRes.getId());

    var payment = paymentMapper.toDomain(paymentReqRes);
    payment.setRelatedOffer(relatedOffer);
    payment.setOwner(owner);

    paymentRepository.save(paymentMapper.toPersistence(payment));

    return payment;
  }

  private BasePaymentRequest buildPaymentRequest(Payer payer) {
    var request = new BasePaymentRequest();
    var unlockProfileAmount = new BigDecimal(amountOfUnlockProfile);

    request.setAmount(unlockProfileAmount);
    request.setCurrency(PaymentCurrency.AR);
    request.setType(PaymentType.PROFILE_UNLOCK);
    request.setPayer(buildPaymentParty(payer));
    request.setDescription(DESCRIPTION_TEMPLATE);

    return request;
  }

  private PaymentParty buildPaymentParty(Payer payer) {
    var party = new PaymentParty();

    party.setCountry("MADAGASCAR");
    party.setPhoneNumber(payer.phoneNumber());
    party.setName(payer.name());

    return party;
  }
}
