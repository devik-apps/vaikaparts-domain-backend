package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.UserType.SELLER;
import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerService {
  private final UserService userService;
  private final SellerMapper sellerMapper;

  public Seller getCurrentSeller() {
    log.info("Retrieving current active seller");
    return sellerMapper.toSeller(userService.getCurrentSeller());
  }

  public Page<Seller> getSellers(Integer page, Integer size) {
    return userService.getJUsers(page, size, SELLER).map(u -> sellerMapper.toSeller((JSeller) u));
  }

  public Seller getSellerById(
      @NotNull @NotBlank(message = "Seller id must not be null") String sellerId) {
    var fetchedUser = userService.findUserById(sellerId);

    if (fetchedUser.getUserType() != SELLER)
      throw new IllegalArgumentException(
          format("No seller corresponds to the provided sellerId=%s", forJava(sellerId)));

    return sellerMapper.toSeller((JSeller) fetchedUser);
  }
}
