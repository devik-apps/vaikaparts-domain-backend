package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.UserType.RESEARCHER;

import com.devikapps.vaikaparts.mapper.user.UserMapper;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResearcherService {
  private final UserService us;
  private final UserMapper um;

  public Researcher getCurrentResearcher() {
    log.info("Retrieving current active researcher");
    return um.toResearcher(us.getCurrentResearcher());
  }

  public Page<Researcher> getResearchers(Integer page, Integer size) {
    return us.getJUsers(page, size, RESEARCHER).map(u -> um.toResearcher((JResearcher) u));
  }
}
