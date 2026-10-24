package com.devikapps.vaikaparts.endpoint.rest.controller.exchange;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.ContactUnlockResponse;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.ManualContactUnlockAdminResponse;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.ManualContactUnlockReviewRequest;
import com.devikapps.vaikaparts.service.ManualContactUnlockService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/contact-unlocks")
@RequiredArgsConstructor
public class ManualContactUnlockController {

  private final ManualContactUnlockService service;

  @GetMapping
  public List<ManualContactUnlockAdminResponse> findPendingReviews() {
    return service.findPendingReviews();
  }

  @PostMapping("/{unlockRequestId}/confirm")
  public ContactUnlockResponse confirm(
      @PathVariable String unlockRequestId,
      @Valid @RequestBody ManualContactUnlockReviewRequest request) {
    return service.confirm(unlockRequestId, request.paymentReference(), request.reviewNote());
  }

  @PostMapping("/{unlockRequestId}/reject")
  public ContactUnlockResponse reject(
      @PathVariable String unlockRequestId,
      @Valid @RequestBody ManualContactUnlockReviewRequest request) {
    return service.reject(unlockRequestId, request.reviewNote());
  }
}
