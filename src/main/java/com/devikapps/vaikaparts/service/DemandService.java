package com.devikapps.vaikaparts.service;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange.RestPart;
import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.file.BucketComponent;
import com.devikapps.vaikaparts.mapper.exchange.DemandMapper;
import com.devikapps.vaikaparts.mapper.exchange.OfferMapper;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.model.exchange.Part;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.service.util.ImageUploader;
import com.devikapps.vaikaparts.service.util.Paginator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandService {

  private static final Duration PRESIGN_DURATION = Duration.ofDays(7);
  private static final String PARTS_BUCKET_PREFIX = "parts/";

  private final DemandRepository demandRepository;
  private final DemandMapper demandMapper;
  private final ResearcherService researcherService;
  private final OfferMapper offerMapper;
  private final OfferRepository offerRepository;
  private final Paginator paginator;
  private final BucketComponent bucketComponent;
  private final ImageUploader imageUploader;
  private final EventProducer<DemandPublishedRequested> demandPublishedRequestedProducer;

  @Transactional
  public Demand createDemand(String description, RestPart restPart) {
    log.info("Creating new demand for authenticated researcher");

    var currentResearcher = researcherService.getCurrentResearcher();
    log.debug("Authenticated researcher: {}", forJava(currentResearcher.getId()));

    validateDemandCreation(description, restPart);

    var partImageBucketKeys = uploadPartImages(restPart);
    var part = buildPartFromRestPart(restPart, partImageBucketKeys);
    var demand = buildNewDemand(description, part, currentResearcher);

    var jDemand = demandMapper.toPersistence(demand);
    jDemand.getPart().setImageBuckets(partImageBucketKeys);
    var savedJDemand = demandRepository.save(jDemand);

    log.info("Successfully created demand with id: {}", forJava(savedJDemand.getId()));
    return demandMapper.toDomain(savedJDemand);
  }

  @Transactional(readOnly = true)
  public Page<Offer> getOffersForDemand(String demandId, Integer page, Integer size) {
    log.info("Fetching offers for demand: {}", forJava(demandId));

    var currentResearcher = researcherService.getCurrentResearcher();

    var jDemand = findDemandByIdAndResearcher(demandId, currentResearcher.getId());

    var pagination = paginator.apply(page, size);
    var pageable =
        PageRequest.of(
            pagination.get("page"), pagination.get("size"), Sort.by("createdAt").descending());

    Page<JOffer> jOffers = offerRepository.findByDemandId(jDemand.getId(), pageable);

    log.info("Found {} offers for demand {}", jOffers.getTotalElements(), forJava(demandId));

    return jOffers.map(offerMapper::toDomain);
  }

  @Transactional
  public Demand updateDemandStatus(String demandId, PostStatus newStatus) {
    log.info(
        "Updating demand status for demand: {} to {}",
        forJava(demandId),
        forJava(newStatus.toString()));

    var currentResearcher = researcherService.getCurrentResearcher();
    var jDemand = findDemandByIdAndResearcher(demandId, currentResearcher.getId());

    validateStatusTransition(jDemand.getStatus(), newStatus);

    boolean shouldNotifySellers = shouldPublishNotifications(newStatus);

    applyStatusUpdate(jDemand, newStatus);

    var updatedJDemand = demandRepository.save(jDemand);

    if (shouldNotifySellers) publishDemandPublishedEvent(updatedJDemand);

    log.info(
        "Successfully updated demand {} to status {}",
        forJava(demandId),
        forJava(newStatus.toString()));

    return demandMapper.toDomain(updatedJDemand);
  }

  @Transactional(readOnly = true)
  public Page<Demand> getResearcherDemandsByStatus(PostStatus status, Integer page, Integer size) {
    var pagination = paginator.apply(page, size);
    var pageable =
        PageRequest.of(
            pagination.get("page"), pagination.get("size"), Sort.by("createdAt").descending());

    log.info(
        "Fetching demands with status: {} for authenticated researcher",
        forJava(status.toString()));

    var currentResearcher = researcherService.getCurrentResearcher();
    var researcherId = currentResearcher.getId();

    log.debug("Retrieving demands for researcher: {}", forJava(researcherId));
    Page<JDemand> jDemands =
        demandRepository.findByResearcherIdAndStatus(researcherId, status, pageable);

    log.info(
        "Found {} demands with status {} for researcher {}",
        jDemands.getTotalElements(),
        forJava(status.toString()),
        forJava(researcherId));

    return jDemands.map(demandMapper::toDomain);
  }

  @Transactional(readOnly = true)
  public Page<Demand> getAllResearcherDemands(Integer page, Integer size) {
    var pagination = paginator.apply(page, size);
    var pageable =
        PageRequest.of(
            pagination.get("page"), pagination.get("size"), Sort.by("createdAt").descending());

    log.info("Fetching all demands for authenticated researcher");

    var currentResearcher = researcherService.getCurrentResearcher();
    var researcherId = currentResearcher.getId();

    Page<JDemand> jDemands =
        demandRepository.findByResearcherIdWithRelations(researcherId, pageable);

    log.info(
        "Found {} total demands for researcher {}",
        jDemands.getTotalElements(),
        forJava(researcherId));

    return jDemands.map(demandMapper::toDomain);
  }

  @Transactional(readOnly = true)
  public Demand getDemandById(String demandId) {
    log.info("Fetching demand by id: {}", forJava(demandId));

    var currentResearcher = researcherService.getCurrentResearcher();
    var jDemand = findDemandByIdAndResearcher(demandId, currentResearcher.getId());

    log.info("Successfully retrieved demand: {}", forJava(demandId));
    return demandMapper.toDomain(jDemand);
  }

  @Transactional(readOnly = true)
  public Demand getDemandByIdWithoutAuthFilter(String demandId) {
    log.info("Fetching demand with id={}", forJava(demandId));
    var jDemand =
        demandRepository
            .findByIdWithRelations(demandId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        format("No demand with id=%s was found.", forJava(demandId))));

    log.info("Successfully retrieved demand with demand id={}", forJava(demandId));
    return demandMapper.toDomain(jDemand);
  }

  private JDemand findDemandByIdAndResearcher(String demandId, String researcherId) {
    return demandRepository
        .findByIdWithRelations(demandId)
        .filter(demand -> demand.getResearcher().getId().equals(researcherId))
        .orElseThrow(
            () -> {
              log.error(
                  "Demand {} not found for researcher {}",
                  forJava(demandId),
                  forJava(researcherId));
              return new ResourceNotFoundException(
                  format("Demand %s not found or access denied", demandId));
            });
  }

  private void validateDemandCreation(String description, RestPart restPart) {
    if (description == null || description.isBlank()) {
      log.error("Demand description is required");
      throw new IllegalArgumentException("Description cannot be null or empty");
    }

    if (restPart == null) {
      log.error("Demand part is required");
      throw new IllegalArgumentException("Part cannot be null");
    }

    log.debug("Demand creation validation passed");
  }

  private void validateStatusTransition(PostStatus currentStatus, PostStatus newStatus) {
    if (currentStatus == newStatus) {
      log.warn("Attempt to update demand to same status: {}", forJava(currentStatus.toString()));
      throw new IllegalStateException(format("Demand is already in %s status", currentStatus));
    }

    if (currentStatus == PostStatus.CANCELED) {
      log.error("Cannot update status of canceled demand");
      throw new IllegalStateException("Cannot update status of a canceled demand");
    }

    if (currentStatus == PostStatus.SUSPENDED && newStatus != PostStatus.PUBLISHED) {
      log.error("Suspended demand can only be published");
      throw new IllegalStateException("Suspended demand can only transition to PUBLISHED status");
    }

    log.debug(
        "Status transition validated: {} -> {}",
        forJava(currentStatus.toString()),
        forJava(newStatus.toString()));
  }

  private boolean shouldPublishNotifications(PostStatus newStatus) {
    return newStatus == PostStatus.PUBLISHED;
  }

  private void publishDemandPublishedEvent(JDemand jDemand) {
    var event =
        DemandPublishedRequested.builder()
            .id(randomUUID().toString())
            .demandId(jDemand.getId())
            .build();

    demandPublishedRequestedProducer.accept(List.of(event));

    log.info("Published DemandPublishedRequested event for demand: {}", forJava(jDemand.getId()));
  }

  @SneakyThrows
  private List<String> uploadPartImages(RestPart restPart) {
    return imageUploader.apply(restPart.images(), PARTS_BUCKET_PREFIX);
  }

  private Part buildPartFromRestPart(RestPart restPart, List<String> imageBucketKeys) {
    var imageUrls =
        imageBucketKeys.stream()
            .map(key -> bucketComponent.presign(key, PRESIGN_DURATION))
            .toList();

    return Part.builder()
        .id(randomUUID().toString())
        .name(restPart.name())
        .carBrand(restPart.carBrand())
        .carModel(restPart.carModel())
        .carYear(restPart.carYear())
        .imageUrls(imageUrls)
        .partCategory(restPart.partCategory())
        .build();
  }

  private Demand buildNewDemand(String description, Part part, Researcher researcher) {
    var now = LocalDateTime.now();

    return Demand.builder()
        .id(randomUUID().toString())
        .description(description)
        .attachedPhotosUrls(new ArrayList<>())
        .part(part)
        .researcher(researcher)
        .status(PostStatus.DRAFT)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private void applyStatusUpdate(JDemand jDemand, PostStatus newStatus) {
    var now = LocalDateTime.now();
    jDemand.setStatus(newStatus);
    jDemand.setUpdatedAt(now);

    switch (newStatus) {
      case CANCELED -> jDemand.setCanceledAt(now);
      case SUSPENDED -> jDemand.setSuspendedAt(now);
      case PUBLISHED -> {
        jDemand.setCanceledAt(null);
        jDemand.setSuspendedAt(null);
      }
    }
  }
}
