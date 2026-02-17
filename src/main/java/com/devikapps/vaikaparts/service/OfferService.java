package com.devikapps.vaikaparts.service;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.RestPartInfo;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.file.BucketComponent;
import com.devikapps.vaikaparts.file.FilenameSanitizer;
import com.devikapps.vaikaparts.mapper.exchange.DemandMapper;
import com.devikapps.vaikaparts.mapper.exchange.OfferMapper;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.model.exchange.Part;
import com.devikapps.vaikaparts.model.exchange.PartInfo;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.service.util.Paginator;
import java.io.File;
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
public class OfferService {

  private static final Duration PRESIGN_DURATION = Duration.ofDays(7);
  private static final String PART_INFO_BUCKET_PREFIX = "part-infos/";

  private final OfferRepository offerRepository;
  private final DemandRepository demandRepository;
  private final OfferMapper offerMapper;
  private final DemandMapper demandMapper;
  private final SellerService sellerService;
  private final Paginator paginator;
  private final BucketComponent bucketComponent;
  private final FilenameSanitizer filenameSanitizer;

  @Transactional
  public Offer createOffer(String demandId, String description, RestPartInfo restPartInfo) {
    log.info("Creating new offer for demand: {}", forJava(demandId));

    var currentSeller = sellerService.getCurrentSeller();
    var jDemand = validateOfferCreation(demandId, description, restPartInfo, currentSeller.getId());

    var imageBucketKeys = uploadPartInfoImages(restPartInfo);
    var partInfo = buildPartInfoFromRestPartInfo(restPartInfo, imageBucketKeys);
    var offer = buildNewOffer(jDemand, description, partInfo, currentSeller);

    var jOffer = offerMapper.toPersistence(offer);
    jOffer.setDemand(jDemand);
    jOffer.getPartInfo().setPartImageBuckets(imageBucketKeys);

    var savedJOffer = offerRepository.save(jOffer);
    log.info("Successfully created offer with id: {}", forJava(savedJOffer.getId()));

    return offerMapper.toDomain(savedJOffer);
  }

  private PartInfo buildPartInfoFromRestPartInfo(
      RestPartInfo restPartInfo, List<String> imageBucketKeys) {
    var imageUrls =
        imageBucketKeys.stream()
            .map(key -> bucketComponent.presign(key, PRESIGN_DURATION))
            .toList();

    var part =
        Part.builder()
            .id(randomUUID().toString())
            .name(restPartInfo.name())
            .carBrand(restPartInfo.carBrand())
            .carModel(restPartInfo.carModel())
            .carYear(restPartInfo.carYear())
            .partCategory(restPartInfo.partCategory())
            .imageUrls(imageUrls)
            .build();

    return PartInfo.builder()
        .id(randomUUID().toString())
        .part(part)
        .condition(restPartInfo.condition())
        .price(restPartInfo.price().doubleValue())
        .build();
  }

  @Transactional
  public Offer updateOfferStatus(String offerId, PostStatus newStatus) {
    log.info(
        "Updating offer status for offer: {} to {}",
        forJava(offerId),
        forJava(newStatus.toString()));

    var currentSeller = sellerService.getCurrentSeller();
    var jOffer = findOfferByIdAndSeller(offerId, currentSeller.getId());

    validateStatusTransition(jOffer.getStatus(), newStatus);
    applyStatusUpdate(jOffer, newStatus);

    var updatedJOffer = offerRepository.save(jOffer);
    log.info(
        "Successfully updated offer {} to status {}",
        forJava(offerId),
        forJava(newStatus.toString()));

    return offerMapper.toDomain(updatedJOffer);
  }

  @Transactional(readOnly = true)
  public Page<Offer> getSellerOffersByStatus(PostStatus status, Integer page, Integer size) {
    log.info(
        "Fetching offers with status: {} for authenticated seller", forJava(status.toString()));

    var pagination = paginator.apply(page, size);
    var pageable =
        PageRequest.of(
            pagination.get("page"), pagination.get("size"), Sort.by("createdAt").descending());

    var currentSeller = sellerService.getCurrentSeller();
    var sellerId = currentSeller.getId();

    var jOffers = offerRepository.findBySellerIdAndStatus(sellerId, status, pageable);
    log.info(
        "Found {} offers with status {} for seller {}",
        jOffers.getTotalElements(),
        forJava(status.toString()),
        forJava(sellerId));

    return jOffers.map(offerMapper::toDomain);
  }

  @Transactional(readOnly = true)
  public Page<Offer> getAllSellerOffers(Integer page, Integer size) {
    log.info("Fetching all offers for authenticated seller");

    var pagination = paginator.apply(page, size);
    var pageable =
        PageRequest.of(
            pagination.get("page"), pagination.get("size"), Sort.by("createdAt").descending());

    var currentSeller = sellerService.getCurrentSeller();
    var sellerId = currentSeller.getId();

    var jOffers = offerRepository.findBySellerIdWithRelations(sellerId, pageable);
    log.info("Found {} total offers for seller {}", jOffers.getTotalElements(), forJava(sellerId));

    return jOffers.map(offerMapper::toDomain);
  }

  @Transactional(readOnly = true)
  public Offer getOfferById(String offerId) {
    log.info("Fetching offer by id: {}", forJava(offerId));

    var currentSeller = sellerService.getCurrentSeller();
    var jOffer = findOfferByIdAndSeller(offerId, currentSeller.getId());

    log.info("Successfully retrieved offer: {}", forJava(offerId));
    return offerMapper.toDomain(jOffer);
  }

  @Transactional(readOnly = true)
  public Page<Offer> getOffersByDemandId(String demandId, Integer page, Integer size) {
    log.info("Fetching offers for demand: {}", forJava(demandId));

    var pagination = paginator.apply(page, size);
    var pageable =
        PageRequest.of(
            pagination.get("page"), pagination.get("size"), Sort.by("createdAt").descending());

    var jOffers = offerRepository.findByDemandId(demandId, pageable);
    log.info("Found {} offers for demand {}", jOffers.getTotalElements(), forJava(demandId));

    return jOffers.map(offerMapper::toDomain);
  }

  private JDemand validateOfferCreation(
      String demandId, String description, RestPartInfo restPartInfo, String sellerId) {

    if (description == null || description.isBlank()) {
      log.error("Offer description is required");
      throw new IllegalArgumentException("Description cannot be null or empty");
    }

    if (restPartInfo == null) {
      log.error("Offer part info is required");
      throw new IllegalArgumentException("Part info cannot be null");
    }

    var jDemand =
        demandRepository
            .findByIdWithRelations(demandId)
            .orElseThrow(
                () -> {
                  log.error("Demand {} not found", forJava(demandId));
                  return new ResourceNotFoundException(format("Demand %s not found", demandId));
                });

    if (jDemand.getStatus() != PostStatus.PUBLISHED) {
      log.error(
          "Demand {} is not published, current status: {}",
          forJava(demandId),
          forJava(jDemand.getStatus().toString()));
      throw new IllegalStateException(
          format(
              "Cannot create offer for demand with status %s, demand must be PUBLISHED",
              jDemand.getStatus()));
    }

    if (offerRepository.existsByDemandIdAndSellerId(demandId, sellerId)) {
      log.error(
          "Seller {} already has an offer for demand {}", forJava(sellerId), forJava(demandId));
      throw new IllegalStateException(
          format("Seller already has an offer for demand %s", demandId));
    }

    log.debug("Offer creation validation passed for demand: {}", forJava(demandId));
    return jDemand;
  }

  private JOffer findOfferByIdAndSeller(String offerId, String sellerId) {
    return offerRepository
        .findByIdWithRelations(offerId)
        .filter(o -> o.getSeller().getId().equals(sellerId))
        .orElseThrow(
            () -> {
              log.error("Offer {} not found for seller {}", forJava(offerId), forJava(sellerId));
              return new ResourceNotFoundException(
                  format("Offer %s not found or access denied", offerId));
            });
  }

  private void validateStatusTransition(PostStatus current, PostStatus next) {
    if (current == next) {
      log.warn("Attempt to update offer to same status: {}", forJava(current.toString()));
      throw new IllegalStateException(format("Offer is already in %s status", current));
    }
    if (current == PostStatus.CANCELED) {
      log.error("Cannot update status of canceled offer");
      throw new IllegalStateException("Cannot update status of a canceled offer");
    }
    if (current == PostStatus.SUSPENDED && next != PostStatus.PUBLISHED) {
      log.error("Suspended offer can only be published");
      throw new IllegalStateException("Suspended offer can only transition to PUBLISHED status");
    }
  }

  private void applyStatusUpdate(JOffer jOffer, PostStatus newStatus) {
    var now = LocalDateTime.now();
    jOffer.setStatus(newStatus);
    jOffer.setUpdatedAt(now);
    switch (newStatus) {
      case CANCELED -> jOffer.setCanceledAt(now);
      case SUSPENDED -> jOffer.setSuspendedAt(now);
      case PUBLISHED -> {
        jOffer.setCanceledAt(null);
        jOffer.setSuspendedAt(null);
      }
    }
  }

  @SneakyThrows
  private List<String> uploadPartInfoImages(RestPartInfo restPartInfo) {
    if (restPartInfo.images() == null || restPartInfo.images().isEmpty()) {
      log.debug("No part info images to upload");
      return new ArrayList<>();
    }

    log.info("Uploading {} part info images to bucket", restPartInfo.images().size());
    var bucketKeys = new ArrayList<String>();

    for (var image : restPartInfo.images()) {
      var originalFilename = image.getOriginalFilename();
      var sanitizedFilename =
          filenameSanitizer.apply(originalFilename != null ? originalFilename : "part-info-image");
      var bucketKey = PART_INFO_BUCKET_PREFIX + randomUUID() + "-" + sanitizedFilename;

      var tempFile = File.createTempFile("part-info-", "-" + sanitizedFilename);
      try {
        image.transferTo(tempFile);
        bucketComponent.upload(tempFile, bucketKey);
        bucketKeys.add(bucketKey);
        log.debug("Successfully uploaded part info image: {}", forJava(bucketKey));
      } finally {
        if (tempFile.exists() && !tempFile.delete()) {
          log.warn("Failed to delete temporary file: {}", forJava(tempFile.getAbsolutePath()));
        }
      }
    }

    log.info("Successfully uploaded {} part info images", bucketKeys.size());
    return bucketKeys;
  }

  private Offer buildNewOffer(
      JDemand jDemand, String description, PartInfo partInfo, Seller currentSeller) {
    var now = LocalDateTime.now();
    var demand = demandMapper.toDomain(jDemand);

    return Offer.builder()
        .id(randomUUID().toString())
        .demand(demand)
        .description(description)
        .attachedPhotosUrls(new ArrayList<>())
        .partsInfo(partInfo)
        .sellerId(currentSeller.getId())
        .status(PostStatus.DRAFT)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
