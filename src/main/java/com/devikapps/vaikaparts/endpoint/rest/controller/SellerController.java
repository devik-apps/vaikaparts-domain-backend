package com.devikapps.vaikaparts.endpoint.rest.controller;

import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sellers")
@RequiredArgsConstructor
public class SellerController {
  private final SellerService sellerService;

  @GetMapping
  public Seller getCurrentSeller() {
    return sellerService.getCurrentSeller();
  }

  @GetMapping("/me")
  public Page<Seller> getSellers(
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {
    return sellerService.getSellers(page, size);
  }
}
