package com.devikapps.vaikaparts.endpoint.rest.controller.user;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.ResearcherContactUnlockSummaryResponse;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.service.ContactUnlockService;
import com.devikapps.vaikaparts.service.ResearcherService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/researchers")
@RequiredArgsConstructor
public class ResearcherController {
  private final ResearcherService rs;
  private final ContactUnlockService contactUnlockService;

  @GetMapping("/me")
  public Researcher getCurrentResearcher() {
    return rs.getCurrentResearcher();
  }

  @GetMapping("/me/contact-unlocks")
  public List<ResearcherContactUnlockSummaryResponse> getMyReleasedContactUnlocks() {
    return contactUnlockService.getReleasedContactUnlocks();
  }

  @GetMapping
  public Page<Researcher> getResearchers(
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {
    return rs.getResearchers(page, size);
  }
}
