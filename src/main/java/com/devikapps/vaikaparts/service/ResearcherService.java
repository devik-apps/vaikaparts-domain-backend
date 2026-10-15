package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.model.classifier.UserType.RESEARCHER;

import com.devikapps.vaikaparts.mapper.user.ResearcherMapper;
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
  private final UserService userService;
  private final ResearcherMapper researcherMapper;

  public Researcher getCurrentResearcher() {
    log.info("Retrieving current active researcher");
    return researcherMapper.toResearcher(userService.getCurrentResearcher());
  }

  public Researcher getOrCreateCurrentResearcher() {
    log.info("Retrieving or creating current active researcher");
    return researcherMapper.toResearcher(userService.getOrCreateCurrentResearcher());
  }

  public Page<Researcher> getResearchers(Integer page, Integer size) {
    return userService
        .getJUsers(page, size, RESEARCHER)
        .map(u -> researcherMapper.toResearcher((JResearcher) u));
  }
}
