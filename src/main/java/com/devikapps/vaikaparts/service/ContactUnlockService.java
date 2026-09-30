package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;
import static com.devikapps.vaikaparts.model.classifier.PostStatus.PUBLISHED;
import static java.util.UUID.randomUUID;

import com.devikapps.vaikaparts.client.PecuniaClient;
import com.devikapps.vaikaparts.client.PecuniaClient.PaymentResponse;
import com.devikapps.vaikaparts.client.PecuniaClientException;
import com.devikapps.vaikaparts.client.PecuniaUnknownOutcomeException;
import com.devikapps.vaikaparts.config.sec.SecContextUtil;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.ContactUnlockResponse;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.SellerContactResponse;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JContactUnlock;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactUnlockService {

  private final UserRepository userRepository;
  private final OfferRepository offerRepository;
  private final ContactUnlockPersistenceService persistence;
  private final PecuniaClient pecuniaClient;
  private final BigDecimal price;

  public ContactUnlockService(
      UserRepository userRepository,
      OfferRepository offerRepository,
      ContactUnlockPersistenceService persistence,
      PecuniaClient pecuniaClient,
      @Value("${contact-unlock.price-mga}") BigDecimal price) {
    if (price == null || price.signum() <= 0) {
      throw new IllegalArgumentException("contact-unlock.price-mga must be positive");
    }
    this.userRepository = userRepository;
    this.offerRepository = offerRepository;
    this.persistence = persistence;
    this.pecuniaClient = pecuniaClient;
    this.price = price;
  }

  public ContactUnlockResponse initiate(String offerId, String provider) {
    if (!"VANILLA_PAY".equals(provider)) {
      throw new IllegalArgumentException("Only VANILLA_PAY is supported");
    }
    var user =
        userRepository
            .findBySupabaseUserId(SecContextUtil.getCurrentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    if (!(user instanceof JResearcher buyer)) {
      throw new IllegalStateException("Only a researcher can unlock seller contacts");
    }
    var offer =
        offerRepository
            .findByIdWithRelations(offerId)
            .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));
    if (offer.getStatus() != PUBLISHED
        || offer.getDemand() == null
        || offer.getDemand().getResearcher() == null
        || !buyer.getId().equals(offer.getDemand().getResearcher().getId())) {
      throw new IllegalStateException("The offer is not unlockable by this researcher");
    }

    var active = persistence.findActive(buyer.getId(), offerId);
    if (active.isPresent()) {
      var existing = active.orElseThrow();
      if (existing.getPaymentUrl() == null && existing.getStatus() == PENDING) {
        PaymentResponse recovered;
        try {
          recovered = pecuniaClient.findByUnlockRequestId(existing.getUnlockRequestId());
        } catch (PecuniaClientException ignored) {
          return map(existing);
        }
        return attachAndMap(existing, recovered, buyer.getId(), offer.getSeller().getId());
      }
      return map(existing);
    }

    var now = OffsetDateTime.now();
    JContactUnlock reserved;
    try {
      reserved =
          persistence.reserve(
              JContactUnlock.builder()
                  .unlockRequestId(randomUUID().toString())
                  .offer(offer)
                  .buyer(buyer)
                  .seller(offer.getSeller())
                  .provider(provider)
                  .status(PENDING)
                  .createdAt(now)
                  .updatedAt(now)
                  .build());
    } catch (DataIntegrityViolationException concurrentAttempt) {
      return persistence
          .findActive(buyer.getId(), offerId)
          .map(this::map)
          .orElseThrow(() -> concurrentAttempt);
    }
    var description = "Unlock seller contact for offer " + offerId;
    PaymentResponse payment;
    try {
      payment =
          pecuniaClient.initiate(
              reserved.getUnlockRequestId(),
              buyer.getId(),
              offer.getSeller().getId(),
              price,
              description);
    } catch (PecuniaUnknownOutcomeException e) {
      payment = pecuniaClient.findByUnlockRequestId(reserved.getUnlockRequestId());
    }
    return attachAndMap(reserved, payment, buyer.getId(), offer.getSeller().getId());
  }

  @Transactional(readOnly = true)
  public SellerContactResponse getSellerContact(String offerId) {
    var user =
        userRepository
            .findBySupabaseUserId(SecContextUtil.getCurrentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    if (!(user instanceof JResearcher buyer)) {
      throw new ResourceNotFoundException("Released contact unlock not found");
    }
    var unlock =
        persistence
            .findActive(buyer.getId(), offerId)
            .filter(candidate -> candidate.getStatus() == RELEASED)
            .orElseThrow(() -> new ResourceNotFoundException("Released contact unlock not found"));
    var sellerUser =
        userRepository
            .findJUserById(unlock.getSeller().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
    if (!(sellerUser instanceof JSeller seller)) {
      throw new ResourceNotFoundException("Seller not found");
    }
    return new SellerContactResponse(seller.getName(), seller.getPhoneNumber(), seller.getEmail());
  }

  private ContactUnlockResponse attachAndMap(
      JContactUnlock reserved, PaymentResponse payment, String buyerId, String sellerId) {
    if (payment == null
        || !reserved.getUnlockRequestId().equals(payment.unlockRequestId())
        || !buyerId.equals(payment.buyerId())
        || !sellerId.equals(payment.sellerId())
        || !"VANILLA_PAY".equals(payment.provider())
        || !"MGA".equals(payment.currency())
        || !"PROFILE_UNLOCK".equals(payment.type())
        || payment.amount() == null
        || payment.amount().compareTo(price) != 0) {
      throw new IllegalStateException("Pecunia payment correlation mismatch");
    }
    return map(persistence.attachPayment(reserved, payment));
  }

  private ContactUnlockResponse map(JContactUnlock unlock) {
    return new ContactUnlockResponse(
        unlock.getUnlockRequestId(), unlock.getStatus(), unlock.getPaymentUrl());
  }
}
