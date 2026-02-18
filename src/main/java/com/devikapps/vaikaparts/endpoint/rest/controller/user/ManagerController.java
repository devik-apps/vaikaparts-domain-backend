package com.devikapps.vaikaparts.endpoint.rest.controller.user;

import com.devikapps.vaikaparts.model.user.Manager;
import com.devikapps.vaikaparts.service.ManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/managers")
@RequiredArgsConstructor
public class ManagerController {
  private final ManagerService ms;

  @GetMapping
  public Manager getCurrentManager() {
    return ms.getCurrent();
  }

  @GetMapping("/me")
  public Page<Manager> getManagers(
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {
    return ms.getManagers(page, size);
  }
}
