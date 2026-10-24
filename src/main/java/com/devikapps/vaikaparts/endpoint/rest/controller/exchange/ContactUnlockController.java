package com.devikapps.vaikaparts.endpoint.rest.controller.exchange;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.ContactUnlockRequest;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.ContactUnlockResponse;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.SellerContactResponse;
import com.devikapps.vaikaparts.service.ContactUnlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/offers/{offerId}/contact-unlocks")
@RequiredArgsConstructor
public class ContactUnlockController {

  private final ContactUnlockService service;

  @PostMapping
  public ResponseEntity<ContactUnlockResponse> initiate(
      @PathVariable String offerId, @Valid @RequestBody ContactUnlockRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.initiate(offerId, request.provider()));
  }

  @GetMapping("/me")
  public SellerContactResponse getSellerContact(@PathVariable String offerId) {
    return service.getSellerContact(offerId);
  }
}
