package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.UserType.SELLER;

import com.devikapps.vaikaparts.mapper.user.UserMapper;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerService {
  private final UserService us;
  private final UserMapper um;

  public Seller getCurrentSeller() {
    log.info("Retrieving current active seller");
    return um.toSeller(us.getCurrentSeller());
  }

  public Page<Seller> getSellers(Integer page, Integer size) {
    return us.getJUsers(page, size, SELLER).map(u -> um.toSeller((JSeller) u));
  }
}
