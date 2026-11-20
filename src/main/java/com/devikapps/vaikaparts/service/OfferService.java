package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.service.util.Paginator.PAGE_FIELD;
import static com.devikapps.vaikaparts.service.util.Paginator.SIZE_FIELD;
import static com.devikapps.vaikaparts.service.util.StatusSwitcher.updateStatus;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange.RestPartInfo;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange.UpdateOfferRequest;
import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.file.BucketComponent;
import com.devikapps.vaikaparts.mapper.exchange.DemandMapper;
import com.devikapps.vaikaparts.mapper.exchange.OfferMapper;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.model.exchange.Part;
import com.devikapps.vaikaparts.model.exchange.PartInfo;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.service.util.ImageUploader;
import com.devikapps.vaikaparts.service.util.Paginator;
import java.math.BigDecimal;
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
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService {

  public static final String CREATED_AT_FIELD = "createdAt";
  private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
  private static final Duration PRESIGN_DURATION = Duration.ofDays(7);
  private static final String PART_INFO_BUCKET_PREFIX = "part-infos/";
  private final OfferRepository offerRepository;
  private final EventProducer<DemandPublishedRequested> demandPublishedRequestedProducer;
  private final DemandRepository demandRepository;
  private final OfferMapper offerMapper;
  private final DemandMapper demandMapper;
  private final SellerService sellerService;
  private final UserService userService;
  private final Paginator paginator;
  private final BucketComponent bucketComponent;
  private final ImageUploader imageUploader;

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

    return mapOfferWithoutResearcherContact(savedJOffer);
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

    var currentUser = userService.getCurrentUser();
    var jOffer =
        switch (currentUser.getUserType()) {
          case SELLER -> findOfferByIdAndSeller(offerId, sellerService.getCurrentSeller().getId());
          case MANAGER -> {
            userService.getCurrentManager();
            yield findOfferById(offerId);
          }
          case RESEARCHER ->
              throw new AuthorizationDeniedException(
                  "Only the owning seller or a manager can update an offer status");
        };

    validateStatusTransition(jOffer.getStatus(), newStatus);
    boolean shouldNotify =
        newStatus == PostStatus.PUBLISHED && jOffer.getStatus() != PostStatus.PUBLISHED;
    applyStatusUpdate(jOffer, newStatus);

    var updatedJOffer = offerRepository.save(jOffer);
    if (shouldNotify) publishOfferNotification(updatedJOffer);
    log.info(
        "Successfully updated offer {} to status {}",
        forJava(offerId),
        forJava(newStatus.toString()));

    return currentUser.getUserType() == UserType.SELLER
        ? mapOfferWithoutResearcherContact(updatedJOffer)
        : offerMapper.toDomain(updatedJOffer);
  }

  @Transactional
  public Offer updateOffer(String offerId, UpdateOfferRequest request) {
    if (request == null) throw new IllegalArgumentException("Offer update cannot be empty");

    var currentUser = userService.getCurrentUser();
    if (currentUser.getUserType() != UserType.MANAGER) {
      throw new AuthorizationDeniedException("Only a manager can update an offer");
    }
    userService.getCurrentManager();

    var jOffer = findOfferById(offerId);
    var partInfoUpdate = request.getPartInfo();
    var jPartInfo = jOffer.getPartInfo();
    if (partInfoUpdate != null && jPartInfo == null) {
      throw new IllegalArgumentException("Offer has no part information to update");
    }

    validateOptionalText(request.getDescription(), "Description", 1000);
    List<MultipartFile> newImages =
        partInfoUpdate == null || partInfoUpdate.getImages() == null
            ? List.of()
            : partInfoUpdate.getImages().stream().filter(image -> !image.isEmpty()).toList();
    List<String> imageKeysToRemove =
        partInfoUpdate == null || partInfoUpdate.getImageKeysToRemove() == null
            ? List.of()
            : partInfoUpdate.getImageKeysToRemove().stream().distinct().toList();
    if (partInfoUpdate != null) {
      validateOptionalText(partInfoUpdate.getName(), "Part name", 255);
      validateOptionalText(partInfoUpdate.getCarBrand(), "Car brand", 255);
      validateOptionalText(partInfoUpdate.getCarModel(), "Car model", 255);
      if (partInfoUpdate.getCarYear() != null
          && (partInfoUpdate.getCarYear().getValue() < 1900
              || partInfoUpdate.getCarYear().getValue() > 2100)) {
        throw new IllegalArgumentException("Car year must be between 1900 and 2100");
      }
      if (partInfoUpdate.getPrice() != null && partInfoUpdate.getPrice().signum() <= 0) {
        throw new IllegalArgumentException("Price must be positive");
      }
    }

    var currentImageBuckets =
        jPartInfo == null || jPartInfo.getPartImageBuckets() == null
            ? List.<String>of()
            : jPartInfo.getPartImageBuckets();
    if (!currentImageBuckets.containsAll(imageKeysToRemove)) {
      throw new IllegalArgumentException("A photo to remove does not belong to this offer");
    }
    var remainingImageBuckets =
        currentImageBuckets.stream().filter(key -> !imageKeysToRemove.contains(key)).toList();
    if (remainingImageBuckets.size() + newImages.size() > 5) {
      throw new IllegalArgumentException("An offer cannot have more than 5 images");
    }
    if (newImages.stream()
        .anyMatch(
            image ->
                !isSupportedImage(image.getContentType())
                    || image.getSize() > MAX_IMAGE_SIZE_BYTES)) {
      throw new IllegalArgumentException(
          "Offer images must be JPEG, PNG, or WebP and no larger than 5 MB");
    }

    boolean changed = false;
    if (request.getDescription() != null
        && !request.getDescription().equals(jOffer.getDescription())) {
      jOffer.setDescription(request.getDescription());
      changed = true;
    }
    if (partInfoUpdate != null) {
      if (partInfoUpdate.getName() != null
          && !partInfoUpdate.getName().equals(jPartInfo.getPartName())) {
        jPartInfo.setPartName(partInfoUpdate.getName());
        changed = true;
      }
      if (partInfoUpdate.getCarBrand() != null
          && !partInfoUpdate.getCarBrand().equals(jPartInfo.getCarBrand())) {
        jPartInfo.setCarBrand(partInfoUpdate.getCarBrand());
        changed = true;
      }
      if (partInfoUpdate.getCarModel() != null
          && !partInfoUpdate.getCarModel().equals(jPartInfo.getCarModel())) {
        jPartInfo.setCarModel(partInfoUpdate.getCarModel());
        changed = true;
      }
      if (partInfoUpdate.getCarYear() != null
          && partInfoUpdate.getCarYear().getValue() != jPartInfo.getCarYear()) {
        jPartInfo.setCarYear(partInfoUpdate.getCarYear().getValue());
        changed = true;
      }
      if (partInfoUpdate.getPartCategory() != null
          && partInfoUpdate.getPartCategory() != jPartInfo.getPartCategory()) {
        jPartInfo.setPartCategory(partInfoUpdate.getPartCategory());
        changed = true;
      }
      if (partInfoUpdate.getCondition() != null
          && partInfoUpdate.getCondition() != jPartInfo.getCondition()) {
        jPartInfo.setCondition(partInfoUpdate.getCondition());
        changed = true;
      }
      if (partInfoUpdate.getPrice() != null
          && partInfoUpdate.getPrice().compareTo(BigDecimal.valueOf(jPartInfo.getPrice())) != 0) {
        jPartInfo.setPrice(partInfoUpdate.getPrice().doubleValue());
        changed = true;
      }
      if (!newImages.isEmpty() || !imageKeysToRemove.isEmpty()) {
        var updatedImages = new ArrayList<>(remainingImageBuckets);
        updatedImages.addAll(imageUploader.apply(newImages, PART_INFO_BUCKET_PREFIX));
        jPartInfo.setPartImageBuckets(updatedImages);
        changed = true;
      }
    }
    if (!changed) throw new IllegalArgumentException("Offer update contains no changes");

    jOffer.setUpdatedAt(LocalDateTime.now());
    var updatedOffer = offerRepository.save(jOffer);
    deletePhotosAfterCommit(imageKeysToRemove, jOffer.getId());
    return offerMapper.toDomain(updatedOffer);
  }

  private void publishOfferNotification(JOffer offer) {
    var event =
        DemandPublishedRequested.builder()
            .id(randomUUID().toString())
            .demandId(offer.getDemand().getId())
            .offerId(offer.getId())
            .build();
    Runnable publish =
        () -> {
          log.info(
              "[NOTIF-PIPELINE][OFFER] Publishing DemandPublishedRequested: eventId={}, offerId={},"
                  + " recipientType=RESEARCHER",
              forJava(event.getId()),
              forJava(event.getOfferId()));
          demandPublishedRequestedProducer.accept(List.of(event));
        };
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              publish.run();
            }
          });
    } else {
      log.warn(
          "[NOTIF-PIPELINE][OFFER] No transaction synchronization: publishing immediately,"
              + " eventId={}",
          forJava(event.getId()));
      publish.run();
    }
  }

  @Transactional(readOnly = true)
  public Page<Offer> getSellerOffersByStatus(PostStatus status, Integer page, Integer size) {
    log.info(
        "Fetching offers with status: {} for authenticated user", forJava(status.toString()));

    var pagination = paginator.apply(page, size);
    var pageable =
        PageRequest.of(
            pagination.get(PAGE_FIELD),
            pagination.get(SIZE_FIELD),
            Sort.by(CREATED_AT_FIELD).descending());

    var currentUser = userService.getCurrentUser();
    var jOffers =
        switch (currentUser.getUserType()) {
          case SELLER ->
              offerRepository.findBySellerIdAndStatus(currentUser.getId(), status, pageable);
          case MANAGER -> offerRepository.findAllByStatusWithRelations(status, pageable);
          case RESEARCHER ->
              throw new IllegalStateException("User is not a SELLER. Actual type: RESEARCHER");
        };
    log.info("Found {} offers with status {}", jOffers.getTotalElements(), forJava(status.toString()));

    return jOffers.map(offer -> currentUser.getUserType() == UserType.SELLER
        ? mapOfferWithoutResearcherContact(offer)
        : offerMapper.toDomain(offer));
  }

  @Transactional(readOnly = true)
  public Page<Offer> getAllSellerOffers(Integer page, Integer size) {
    log.info("Fetching all offers for authenticated user");

    var pagination = paginator.apply(page, size);
    var pageable =
        PageRequest.of(
            pagination.get(PAGE_FIELD),
            pagination.get(SIZE_FIELD),
            Sort.by(CREATED_AT_FIELD).descending());

    var currentUser = userService.getCurrentUser();
    var jOffers =
        switch (currentUser.getUserType()) {
          case SELLER -> offerRepository.findBySellerIdWithRelations(currentUser.getId(), pageable);
          case MANAGER -> offerRepository.findAllWithRelations(pageable);
          case RESEARCHER ->
              throw new IllegalStateException("User is not a SELLER. Actual type: RESEARCHER");
        };
    log.info("Found {} total offers", jOffers.getTotalElements());

    return jOffers.map(offer -> currentUser.getUserType() == UserType.SELLER
        ? mapOfferWithoutResearcherContact(offer)
        : offerMapper.toDomain(offer));
  }

  @Transactional(readOnly = true)
  public Offer getOfferById(String offerId) {
    log.info("Fetching offer by id: {}", forJava(offerId));

    var currentSeller = sellerService.getCurrentSeller();
    var jOffer = findOfferByIdAndSeller(offerId, currentSeller.getId());

    log.info("Successfully retrieved offer: {}", forJava(offerId));
    return mapOfferWithoutResearcherContact(jOffer);
  }

  @Transactional(readOnly = true)
  public Offer getOfferByIdWithoutAuthFilter(String offerId) {
    log.info("Fetching offer without authentication filter: {}", forJava(offerId));

    var jOffer =
        offerRepository
            .findByIdWithRelations(offerId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        format("No offer with id=%s was found.", forJava(offerId))));

    return mapOfferWithoutResearcherContact(jOffer);
  }

  @Transactional(readOnly = true)
  public Page<Offer> getOffersByDemandId(String demandId, Integer page, Integer size) {
    log.info("Fetching offers for demand: {}", forJava(demandId));

    var pagination = paginator.apply(page, size);
    var pageable =
        PageRequest.of(
            pagination.get(PAGE_FIELD),
            pagination.get(SIZE_FIELD),
            Sort.by(CREATED_AT_FIELD).descending());

    var jOffers = offerRepository.findByDemandId(demandId, pageable);
    log.info("Found {} offers for demand {}", jOffers.getTotalElements(), forJava(demandId));

    return jOffers.map(this::mapOfferWithoutResearcherContact);
  }

  private Offer mapOfferWithoutResearcherContact(JOffer jOffer) {
    var offer = offerMapper.toDomain(jOffer);
    if (offer.getDemand() != null && offer.getDemand().getResearcher() != null) {
      var researcher = offer.getDemand().getResearcher();
      researcher.setSupabaseUserId(null);
      researcher.setName(null);
      researcher.setEmail(null);
      researcher.setPhoneNumber(null);
      researcher.setProfileImgUrl(null);
    }
    return offer;
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
    var offer = findOfferById(offerId);
    if (!offer.getSeller().getId().equals(sellerId)) {
      throwOfferNotFound(offerId, sellerId);
    }
    return offer;
  }

  private JOffer findOfferById(String offerId) {
    return offerRepository
        .findByIdWithRelations(offerId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    format("Offer %s not found", forJava(offerId))));
  }

  private void throwOfferNotFound(String offerId, String sellerId) {
    log.error("Offer {} not found for seller {}", forJava(offerId), forJava(sellerId));
    throw new ResourceNotFoundException(format("Offer %s not found or access denied", offerId));
  }

  private boolean isSupportedImage(String contentType) {
    return "image/jpeg".equals(contentType)
        || "image/png".equals(contentType)
        || "image/webp".equals(contentType);
  }

  private void validateOptionalText(String value, String field, int maxLength) {
    if (value != null && value.isBlank()) {
      throw new IllegalArgumentException(field + " cannot be blank");
    }
    if (value != null && value.length() > maxLength) {
      throw new IllegalArgumentException(field + " cannot exceed " + maxLength + " characters");
    }
  }

  private void deletePhotosAfterCommit(List<String> imageKeys, String offerId) {
    if (imageKeys.isEmpty()) return;
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            for (String imageKey : imageKeys) {
              try {
                bucketComponent.delete(imageKey);
              } catch (Exception e) {
                log.warn(
                    "Failed to delete removed photo from offer {} after database update",
                    forJava(offerId),
                    e);
              }
            }
          }
        });
  }

  private void validateStatusTransition(PostStatus current, PostStatus next) {
    if (next == PostStatus.CLOSED || next == PostStatus.SATISFIED) {
      throw new IllegalStateException(
          "Only the researcher who owns the demand can resolve an offer");
    }
    if (current == next) {
      log.warn("Attempt to update offer to same status: {}", forJava(current.toString()));
      throw new IllegalStateException(format("Offer is already in %s status", current));
    }
    if (current == PostStatus.CANCELED) {
      log.error("Cannot update status of canceled offer");
      throw new IllegalStateException("Cannot update status of a canceled offer");
    }
    if (isResolved(current)) {
      log.error("Cannot update status of terminal offer");
      throw new IllegalStateException("Cannot update status of a terminal offer");
    }
    if (current == PostStatus.SUSPENDED && next != PostStatus.PUBLISHED) {
      log.error("Suspended offer can only be published");
      throw new IllegalStateException("Suspended offer can only transition to PUBLISHED status");
    }
  }

  private boolean isResolved(PostStatus status) {
    return status == PostStatus.CLOSED || status == PostStatus.SATISFIED;
  }

  private void applyStatusUpdate(JOffer jOffer, PostStatus newStatus) {
    updateStatus(jOffer, newStatus);
  }

  @SneakyThrows
  private List<String> uploadPartInfoImages(RestPartInfo restPartInfo) {
    return imageUploader.apply(restPartInfo.images(), PART_INFO_BUCKET_PREFIX);
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
