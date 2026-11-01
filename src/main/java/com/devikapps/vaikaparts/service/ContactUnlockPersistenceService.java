package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING_MANUAL_REVIEW;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;

import com.devikapps.vaikaparts.client.PecuniaClient.PapiPaymentResponse;
import com.devikapps.vaikaparts.client.PecuniaClient.PaymentResponse;
import com.devikapps.vaikaparts.repository.ContactUnlockRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JContactUnlock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactUnlockPersistenceService {

  private final ContactUnlockRepository repository;

  @Transactional(readOnly = true)
  public Optional<JContactUnlock> findActive(String buyerId, String offerId) {
    return repository.findByBuyerIdAndOfferIdAndStatusIn(
        buyerId, offerId, List.of(PENDING, PENDING_MANUAL_REVIEW, RELEASED));
  }

  @Transactional(readOnly = true)
  public Optional<JContactUnlock> findOwned(
      String buyerId, String offerId, String unlockRequestId) {
    return repository.findByUnlockRequestIdAndBuyerIdAndOfferId(unlockRequestId, buyerId, offerId);
  }

  @Transactional
  public JContactUnlock reserve(JContactUnlock unlock) {
    return repository.saveAndFlush(unlock);
  }

  @Transactional
  public JContactUnlock attachPayment(JContactUnlock reserved, PaymentResponse payment) {
    var current =
        repository.findByUnlockRequestIdForUpdate(reserved.getUnlockRequestId()).orElseThrow();
    current.setPaymentId(payment.paymentId());
    current.setPaymentUrl(payment.paymentUrl());
    current.setUpdatedAt(OffsetDateTime.now());
    return repository.save(current);
  }

  @Transactional
  public JContactUnlock attachPapiPayment(JContactUnlock reserved, PapiPaymentResponse payment) {
    var current =
        repository.findByUnlockRequestIdForUpdate(reserved.getUnlockRequestId()).orElseThrow();
    if (current.getPaymentId() != null && !current.getPaymentId().equals(payment.id())) {
      throw new IllegalStateException("Pecunia PAPI payment ID does not match the reserved unlock");
    }
    current.setPaymentId(payment.id());
    current.setUpdatedAt(OffsetDateTime.now());
    return repository.save(current);
  }

  @Transactional
  public JContactUnlock failPapi(JContactUnlock unlock, String paymentId) {
    var current =
        repository.findByUnlockRequestIdForUpdate(unlock.getUnlockRequestId()).orElseThrow();
    if (current.getPaymentId() != null && !current.getPaymentId().equals(paymentId)) {
      throw new IllegalStateException("Pecunia PAPI payment ID does not match the reserved unlock");
    }
    if (current.getStatus() == PENDING) {
      current.setPaymentId(paymentId);
      current.setStatus(com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.FAILED);
      current.setUpdatedAt(OffsetDateTime.now());
      return repository.save(current);
    }
    return current;
  }
}
