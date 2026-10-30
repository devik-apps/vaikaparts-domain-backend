package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING_MANUAL_REVIEW;
import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;
import static com.devikapps.vaikaparts.model.classifier.PostStatus.PUBLISHED;
import static java.util.UUID.randomUUID;

import com.devikapps.vaikaparts.client.PecuniaClient;
import com.devikapps.vaikaparts.client.PecuniaClient.PapiPaymentResponse;
import com.devikapps.vaikaparts.client.PecuniaClient.PaymentResponse;
import com.devikapps.vaikaparts.client.PecuniaClientException;
import com.devikapps.vaikaparts.client.PecuniaNotFoundException;
import com.devikapps.vaikaparts.client.PecuniaUnknownOutcomeException;
import com.devikapps.vaikaparts.config.sec.SecContextUtil;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.ContactUnlockRequest;
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
  private final ContactReleaseService contactReleaseService;
  private final BigDecimal price;
  private final String manualPaymentPhoneNumber;

  public ContactUnlockService(
      UserRepository userRepository,
      OfferRepository offerRepository,
      ContactUnlockPersistenceService persistence,
      PecuniaClient pecuniaClient,
      ContactReleaseService contactReleaseService,
      @Value("${contact-unlock.price-mga}") BigDecimal price,
      @Value("${contact-unlock.manual.orange-money-number:}") String manualPaymentPhoneNumber) {
    if (price == null || price.signum() <= 0) {
      throw new IllegalArgumentException("contact-unlock.price-mga must be positive");
    }
    this.userRepository = userRepository;
    this.offerRepository = offerRepository;
    this.persistence = persistence;
    this.pecuniaClient = pecuniaClient;
    this.contactReleaseService = contactReleaseService;
    this.price = price;
    this.manualPaymentPhoneNumber = manualPaymentPhoneNumber;
  }

  public ContactUnlockResponse initiate(String offerId, String provider) {
    return initiate(offerId, new ContactUnlockRequest(provider));
  }

  public ContactUnlockResponse getUnlockStatus(String offerId, String unlockRequestId) {
    var user =
        userRepository
            .findBySupabaseUserId(SecContextUtil.getCurrentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    if (!(user instanceof JResearcher buyer)) {
      throw new ResourceNotFoundException("Contact unlock not found");
    }
    var unlock =
        persistence
            .findOwned(buyer.getId(), offerId, unlockRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("Contact unlock not found"));
    if (!"PAPI".equals(unlock.getProvider()) || unlock.getStatus() != PENDING) return map(unlock);

    var payment = pecuniaClient.findPapiByReference(unlockRequestId);
    validatePapiPayment(unlock, payment, unlock.getPaymentMethod(), null);
    unlock = persistence.attachPapiPayment(unlock, payment);
    return switch (payment.status()) {
      case "PENDING" -> map(unlock);
      case "FAILED" -> map(persistence.failPapi(unlock, payment.id()));
      case "SUCCESS" -> {
        contactReleaseService.releasePapi(unlockRequestId, payment.id());
        unlock.setStatus(RELEASED);
        yield map(unlock);
      }
      default -> throw new IllegalStateException("Pecunia returned an unsupported PAPI status");
    };
  }

  public ContactUnlockResponse initiate(String offerId, ContactUnlockRequest request) {
    var provider = request.provider();
    var papi = "PAPI".equals(provider);
    if (!papi && !"VANILLA_PAY".equals(provider) && !"MANUAL_ORANGE_MONEY".equals(provider)) {
      throw new IllegalArgumentException("Unsupported contact unlock provider");
    }
    if (papi) {
      if (request.paymentMethod() == null
          || !java.util.Set.of("MVOLA", "AIRTEL_MONEY", "ORANGE_MONEY")
              .contains(request.paymentMethod())) {
        throw new IllegalArgumentException("Unsupported PAPI payment method");
      }
      if ("ORANGE_MONEY".equals(request.paymentMethod())
          != (request.temporaryCode() != null && !request.temporaryCode().isBlank())) {
        throw new IllegalArgumentException("Orange Money requires a temporary code only");
      }
      if (price.compareTo(BigDecimal.valueOf(300)) < 0 || price.stripTrailingZeros().scale() > 0) {
        throw new IllegalStateException(
            "PAPI contact unlock price must be an integer of at least 300 MGA");
      }
    } else if (request.paymentMethod() != null || request.temporaryCode() != null) {
      throw new IllegalArgumentException("PAPI payment details are only valid for PAPI");
    }
    if ("MANUAL_ORANGE_MONEY".equals(provider)
        && (manualPaymentPhoneNumber == null || manualPaymentPhoneNumber.isBlank())) {
      throw new IllegalStateException("Manual Orange Money number is not configured");
    }
    var user =
        userRepository
            .findBySupabaseUserId(SecContextUtil.getCurrentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    if (!(user instanceof JResearcher buyer)) {
      throw new IllegalStateException("Only a researcher can unlock seller contacts");
    }
    if (papi
        && (blank(buyer.getName())
            || buyer.getName().length() > 255
            || !validMadagascarPhone(buyer.getPhoneNumber()))) {
      throw new IllegalStateException(
          "Complete your name and phone number before paying with PAPI");
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
      if ("PAPI".equals(existing.getProvider())) {
        if (!request.paymentMethod().equals(existing.getPaymentMethod())) return map(existing);
        try {
          return getUnlockStatus(offerId, existing.getUnlockRequestId());
        } catch (PecuniaNotFoundException notCreatedYet) {
          var recovered =
              pecuniaClient.initiatePapi(
                  existing.getUnlockRequestId(),
                  existing.getPaymentAmount(),
                  request.paymentMethod(),
                  buyer.getName(),
                  buyer.getPhoneNumber(),
                  request.temporaryCode());
          return attachAndMapPapi(existing, recovered, request.paymentMethod(), buyer);
        } catch (PecuniaClientException unavailable) {
          return map(existing);
        }
      }
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
                  .paymentMethod(papi ? request.paymentMethod() : null)
                  .paymentAmount(papi ? price : null)
                  .status("MANUAL_ORANGE_MONEY".equals(provider) ? PENDING_MANUAL_REVIEW : PENDING)
                  .createdAt(now)
                  .updatedAt(now)
                  .build());
    } catch (DataIntegrityViolationException concurrentAttempt) {
      return persistence
          .findActive(buyer.getId(), offerId)
          .map(this::map)
          .orElseThrow(() -> concurrentAttempt);
    }
    if ("MANUAL_ORANGE_MONEY".equals(provider)) {
      return map(reserved);
    }
    if (papi) {
      PapiPaymentResponse payment;
      try {
        payment =
            pecuniaClient.initiatePapi(
                reserved.getUnlockRequestId(),
                price,
                request.paymentMethod(),
                buyer.getName(),
                buyer.getPhoneNumber(),
                request.temporaryCode());
      } catch (PecuniaUnknownOutcomeException e) {
        try {
          payment = pecuniaClient.findPapiByReference(reserved.getUnlockRequestId());
        } catch (PecuniaNotFoundException notCreatedYet) {
          try {
            payment =
                pecuniaClient.initiatePapi(
                    reserved.getUnlockRequestId(),
                    price,
                    request.paymentMethod(),
                    buyer.getName(),
                    buyer.getPhoneNumber(),
                    request.temporaryCode());
          } catch (PecuniaClientException retryUnavailable) {
            return map(reserved);
          }
        } catch (PecuniaClientException unavailable) {
          return map(reserved);
        }
      }
      return attachAndMapPapi(reserved, payment, request.paymentMethod(), buyer);
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

  private ContactUnlockResponse attachAndMapPapi(
      JContactUnlock reserved,
      PapiPaymentResponse payment,
      String paymentMethod,
      JResearcher buyer) {
    validatePapiPayment(reserved, payment, paymentMethod, buyer);
    return map(persistence.attachPapiPayment(reserved, payment));
  }

  private void validatePapiPayment(
      JContactUnlock unlock,
      PapiPaymentResponse payment,
      String expectedPaymentMethod,
      JResearcher buyer) {
    if (payment == null
        || blank(payment.id())
        || !unlock.getUnlockRequestId().equals(payment.transactionId())
        || !unlock.getUnlockRequestId().equals(payment.merchantReference())
        || !"PAPI".equals(payment.provider())
        || !"MGA".equals(payment.currency())
        || !"PROFILE_UNLOCK".equals(payment.type())
        || payment.amount() == null
        || unlock.getPaymentAmount() == null
        || payment.amount().compareTo(unlock.getPaymentAmount()) != 0
        || payment.status() == null
        || !java.util.Set.of("PENDING", "SUCCESS", "FAILED").contains(payment.status())
        || payment.paymentMethod() == null
        || !java.util.Set.of("MVOLA", "AIRTEL_MONEY", "ORANGE_MONEY")
            .contains(payment.paymentMethod())
        || (expectedPaymentMethod != null
            && !expectedPaymentMethod.equals(payment.paymentMethod()))) {
      throw new IllegalStateException("Pecunia PAPI payment correlation mismatch");
    }
    if (buyer != null
        && (payment.payer() == null
            || !buyer.getName().equals(payment.payer().name())
            || !normalizeMadagascarPhone(buyer.getPhoneNumber())
                .equals(normalizeMadagascarPhone(payment.payer().phoneNumber())))) {
      throw new IllegalStateException("Pecunia PAPI payer does not match the researcher profile");
    }
  }

  private ContactUnlockResponse map(JContactUnlock unlock) {
    var manual = "MANUAL_ORANGE_MONEY".equals(unlock.getProvider());
    return new ContactUnlockResponse(
        unlock.getUnlockRequestId(),
        unlock.getStatus(),
        unlock.getPaymentUrl(),
        manual ? manualPaymentPhoneNumber : null,
        manual ? price : "PAPI".equals(unlock.getProvider()) ? unlock.getPaymentAmount() : null,
        manual || "PAPI".equals(unlock.getProvider()) ? "MGA" : null);
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private String normalizeMadagascarPhone(String phoneNumber) {
    var digits = phoneNumber.replaceAll("[^0-9]", "");
    if (digits.startsWith("261") && digits.length() == 12) return "0" + digits.substring(3);
    if (digits.length() == 9) return "0" + digits;
    return digits;
  }

  private boolean validMadagascarPhone(String phoneNumber) {
    if (blank(phoneNumber)) return false;
    var digits = phoneNumber.replaceAll("[^0-9]", "");
    return digits.matches("(?:[0-9]{9}|0[0-9]{9}|261[0-9]{9})");
  }
}
