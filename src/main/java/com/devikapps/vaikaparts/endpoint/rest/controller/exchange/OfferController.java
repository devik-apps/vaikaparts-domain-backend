package com.devikapps.vaikaparts.endpoint.rest.controller.exchange;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange.CreateOfferRequest;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/offers")
@RequiredArgsConstructor
public class OfferController {

  private final OfferService offerService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Offer> createOffer(@Valid @ModelAttribute CreateOfferRequest request) {
    var offer =
        offerService.createOffer(request.demandId(), request.description(), request.partInfo());
    return ResponseEntity.status(HttpStatus.CREATED).body(offer);
  }

  @PatchMapping("/{offerId}/status")
  public Offer updateOfferStatus(@PathVariable String offerId, @RequestParam PostStatus status) {

    return offerService.updateOfferStatus(offerId, status);
  }

  @GetMapping
  public Page<Offer> getSellerOffers(
      @RequestParam(required = false) PostStatus status,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {

    return status != null
        ? offerService.getSellerOffersByStatus(status, page, size)
        : offerService.getAllSellerOffers(page, size);
  }

  @GetMapping("/{offerId}")
  public Offer getOfferById(@PathVariable String offerId) {
    return offerService.getOfferById(offerId);
  }

  @GetMapping("/demand/{demandId}")
  public Page<Offer> getOffersByDemandId(
      @PathVariable String demandId,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {

    return offerService.getOffersByDemandId(demandId, page, size);
  }
}
