package com.devikapps.vaikaparts.service;

import com.devikapps.vaikaparts.mapper.user.UserMapper;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.user.Manager;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerService {
  private final UserService us;
  private final UserMapper um;

  public Manager getCurrent() {
    log.info("Retrieving current active manager");
    return um.toManager(us.getCurrentManager());
  }

  public Page<Manager> getManagers(Integer page, Integer size) {
    return us.getJUsers(page, size, UserType.MANAGER).map(u -> um.toManager((JManager) u));
  }
}
