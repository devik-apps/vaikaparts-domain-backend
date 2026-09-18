package com.devikapps.vaikaparts.endpoint.rest.controller.exchange;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange.CreateDemandRequest;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.service.DemandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/demands")
@RequiredArgsConstructor
public class DemandController {
  private final DemandService demandService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Demand> createDemand(
      @Valid @ModelAttribute CreateDemandRequest request,
      @RequestParam(name = "published", required = false, defaultValue = "false") Boolean published) {
    var demand = demandService.createDemand(request.description(), request.part());
    if (published) {
      demand = demandService.updateDemandStatus(demand.getId(), PostStatus.PUBLISHED);
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(demand);
  }

  @GetMapping("/all")
  public Page<Demand> getAllDemands(
          @RequestParam(name = "page", required = false) Integer page,
          @RequestParam(name = "size", required = false) Integer size
  ){
    return demandService.getAllDemands(page,size);
  }

  @GetMapping
  public Page<Demand> getResearcherDemands(
      @RequestParam(name = "status", required = false) PostStatus status,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {

    return status != null
        ? demandService.getResearcherDemandsByStatus(status, page, size)
        : demandService.getAllResearcherDemands(page, size);
  }

  @PatchMapping("/{demandId}/status")
  public Demand updateDemandStatus(@PathVariable String demandId, @RequestParam PostStatus status) {
    return demandService.updateDemandStatus(demandId, status);
  }

  @GetMapping("/{demandId}/offers")
  public Page<Offer> getOffersForDemand(
      @PathVariable String demandId,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {

    return demandService.getOffersForDemand(demandId, page, size);
  }

  @GetMapping("/{demandId}")
  public Demand getDemandById(@PathVariable String demandId) {
    return demandService.getDemandById(demandId);
  }
}
