package com.devikapps.vaikaparts.service;

import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange.RestPart;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PartCondition;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.repository.DemandPublishedRequestedRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.exchange.JPartInfo;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class DemandServiceIT extends FacadeIT {

  private static final String TEST_RESEARCHER_ID = "researcher-123";
  private static final String TEST_RESEARCHER_NAME = "John Doe";
  private static final String TEST_PHONE = "+1234567890";
  private static final String TEST_EMAIL = "test@gmail.com";
  private static final String TEST_SUPABASE_USER_ID = "supabase-user-123";
  private static final String TEST_DESCRIPTION = "Looking for a headlight for Toyota Corolla 2015";
  private static final String TEST_PART_NAME = "Headlight";
  private static final String TEST_CAR_BRAND = "Toyota";
  private static final String TEST_CAR_MODEL = "Corolla";
  private static final int TEST_CAR_YEAR = 2015;
  private static final double TEST_PRICE = 150.00;

  private static final Integer DEFAULT_PAGE = 0;
  private static final Integer DEFAULT_SIZE = 10;

  @Autowired private DemandService demandService;
  @Autowired private DemandRepository demandRepository;
  @Autowired private DemandPublishedRequestedRepository demandPublishedRequestedRepository;
  @Autowired private OfferRepository offerRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ValueObjectMapper vom;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private NotificationRequestedRepository notificationRequestedRepository;

  private JResearcher testResearcher;

  @BeforeEach
  void setUp() {
    testResearcher = createTestResearcher();
    authenticateResearcher();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    notificationRepository.deleteAll();
    notificationRequestedRepository.deleteAll();
    demandPublishedRequestedRepository.deleteAll();
    offerRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void should_create_demand_successfully() {
    val restPart = buildTestRestPart();

    val demand = demandService.createDemand(TEST_DESCRIPTION, restPart);

    assertNotNull(demand);
    assertNotNull(demand.getId());
    assertEquals(TEST_DESCRIPTION, demand.getDescription());
    assertEquals(PostStatus.DRAFT, demand.getStatus());
    assertEquals(TEST_RESEARCHER_ID, demand.getResearcher().getId());
    assertEquals(TEST_RESEARCHER_NAME, demand.getResearcher().getName());
    assertEquals(TEST_EMAIL, demand.getResearcher().getEmail());
    assertEquals(TEST_PART_NAME, demand.getPart().getName());
    assertEquals(TEST_CAR_BRAND, demand.getPart().getCarBrand());
    assertEquals(TEST_CAR_MODEL, demand.getPart().getCarModel());
    assertEquals(Year.of(TEST_CAR_YEAR), demand.getPart().getCarYear());
    assertEquals(PartCategory.FOG_LIGHTS, demand.getPart().getPartCategory());
    assertNotNull(demand.getCreatedAt());
    assertNotNull(demand.getUpdatedAt());
    assertNull(demand.getCanceledAt());
    assertNull(demand.getSuspendedAt());

    val savedDemand = demandRepository.findById(demand.getId());
    assertTrue(savedDemand.isPresent());
    assertEquals(TEST_DESCRIPTION, savedDemand.get().getDescription());
    assertEquals(PostStatus.DRAFT, savedDemand.get().getStatus());
  }

  @Test
  @Transactional
  void should_create_demand_with_single_image_successfully() {
    val imageFile =
        new MockMultipartFile(
            "image", "headlight.jpg", "image/jpeg", "fake image content".getBytes());

    val restPart =
        new RestPart(
            TEST_PART_NAME,
            TEST_CAR_BRAND,
            TEST_CAR_MODEL,
            Year.of(TEST_CAR_YEAR),
            List.of(imageFile),
            PartCategory.FOG_LIGHTS);

    val demand = demandService.createDemand(TEST_DESCRIPTION, restPart);

    assertNotNull(demand);
    assertNotNull(demand.getPart().getImageUrls());
    assertEquals(1, demand.getPart().getImageUrls().size());

    val savedDemand = demandRepository.findById(demand.getId());
    assertTrue(savedDemand.isPresent());
    assertNotNull(savedDemand.get().getPart().getImageBuckets());
    assertEquals(1, savedDemand.get().getPart().getImageBuckets().size());
  }

  @Test
  @Transactional
  void should_create_demand_with_multiple_images_successfully() {
    val imageFile1 =
        new MockMultipartFile(
            "image1", "headlight-front.jpg", "image/jpeg", "content 1".getBytes());
    val imageFile2 =
        new MockMultipartFile("image2", "headlight-side.jpg", "image/jpeg", "content 2".getBytes());
    val imageFile3 =
        new MockMultipartFile("image3", "headlight-back.jpg", "image/jpeg", "content 3".getBytes());

    val restPart =
        new RestPart(
            TEST_PART_NAME,
            TEST_CAR_BRAND,
            TEST_CAR_MODEL,
            Year.of(TEST_CAR_YEAR),
            List.of(imageFile1, imageFile2, imageFile3),
            PartCategory.FOG_LIGHTS);

    val demand = demandService.createDemand(TEST_DESCRIPTION, restPart);

    assertNotNull(demand);
    assertNotNull(demand.getPart().getImageUrls());
    assertEquals(3, demand.getPart().getImageUrls().size());

    val savedDemand = demandRepository.findById(demand.getId());
    assertTrue(savedDemand.isPresent());
    assertEquals(3, savedDemand.get().getPart().getImageBuckets().size());
  }

  @Test
  @Transactional
  void should_create_demand_without_images_successfully() {
    val restPart =
        new RestPart(
            TEST_PART_NAME,
            TEST_CAR_BRAND,
            TEST_CAR_MODEL,
            Year.of(TEST_CAR_YEAR),
            new ArrayList<>(),
            PartCategory.FOG_LIGHTS);

    val demand = demandService.createDemand(TEST_DESCRIPTION, restPart);

    assertNotNull(demand);
    assertNotNull(demand.getPart().getImageUrls());
    assertTrue(demand.getPart().getImageUrls().isEmpty());

    val savedDemand = demandRepository.findById(demand.getId());
    assertTrue(savedDemand.isPresent());
    assertTrue(savedDemand.get().getPart().getImageBuckets().isEmpty());
  }

  @Test
  void should_throw_exception_when_description_is_null() {
    val restPart = buildTestRestPart();

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> demandService.createDemand(null, restPart));

    assertTrue(exception.getMessage().contains("cannot be null or empty"));
  }

  @Test
  void should_throw_exception_when_description_is_blank() {
    val restPart = buildTestRestPart();

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> demandService.createDemand("   ", restPart));

    assertTrue(exception.getMessage().contains("cannot be null or empty"));
  }

  @Test
  void should_throw_exception_when_part_is_null() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> demandService.createDemand(TEST_DESCRIPTION, null));

    assertTrue(exception.getMessage().contains("Part cannot be null"));
  }

  @Test
  void should_update_demand_status_to_published() throws InterruptedException {
    val demand = createTestDemand(PostStatus.DRAFT);

    val updatedDemand = demandService.updateDemandStatus(demand.getId(), PostStatus.PUBLISHED);

    assertEquals(PostStatus.PUBLISHED, updatedDemand.getStatus());
    assertNull(updatedDemand.getCanceledAt());
    assertNull(updatedDemand.getSuspendedAt());

    val savedDemand = demandRepository.findById(demand.getId());
    assertTrue(savedDemand.isPresent());
    assertEquals(PostStatus.PUBLISHED, savedDemand.get().getStatus());

    Thread.sleep(1000);

    val eventLogs = demandPublishedRequestedRepository.findAll();
    assertEquals(1, eventLogs.size());
    assertEquals(demand.getId(), eventLogs.getFirst().getDemand().getId());
  }

  @Test
  void should_not_publish_event_when_republishing() {
    val demand = createTestDemand(PostStatus.PUBLISHED);
    demandRepository.save(demand);

    demandService.updateDemandStatus(demand.getId(), PostStatus.SUSPENDED);
    demandService.updateDemandStatus(demand.getId(), PostStatus.PUBLISHED);

    val eventLogs = demandPublishedRequestedRepository.findAll();
    assertEquals(0, eventLogs.size());
  }

  @Test
  void should_update_demand_status_to_canceled() {
    val demand = createTestDemand(PostStatus.PUBLISHED);

    val updatedDemand = demandService.updateDemandStatus(demand.getId(), PostStatus.CANCELED);

    assertEquals(PostStatus.CANCELED, updatedDemand.getStatus());
    assertNotNull(updatedDemand.getCanceledAt());

    val savedDemand = demandRepository.findById(demand.getId());
    assertTrue(savedDemand.isPresent());
    assertEquals(PostStatus.CANCELED, savedDemand.get().getStatus());
    assertNotNull(savedDemand.get().getCanceledAt());
  }

  @Test
  void should_update_demand_status_to_suspended() {
    val demand = createTestDemand(PostStatus.PUBLISHED);

    val updatedDemand = demandService.updateDemandStatus(demand.getId(), PostStatus.SUSPENDED);

    assertEquals(PostStatus.SUSPENDED, updatedDemand.getStatus());
    assertNotNull(updatedDemand.getSuspendedAt());

    val savedDemand = demandRepository.findById(demand.getId());
    assertTrue(savedDemand.isPresent());
    assertEquals(PostStatus.SUSPENDED, savedDemand.get().getStatus());
    assertNotNull(savedDemand.get().getSuspendedAt());
  }

  @Test
  void should_update_suspended_demand_to_published() {
    val demand = createTestDemand(PostStatus.SUSPENDED);

    val updatedDemand = demandService.updateDemandStatus(demand.getId(), PostStatus.PUBLISHED);

    assertEquals(PostStatus.PUBLISHED, updatedDemand.getStatus());
    assertNull(updatedDemand.getSuspendedAt());
    assertNull(updatedDemand.getCanceledAt());
  }

  @Test
  void should_throw_exception_when_updating_to_same_status() {
    val demand = createTestDemand(PostStatus.PUBLISHED);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> demandService.updateDemandStatus(demand.getId(), PostStatus.PUBLISHED));

    assertTrue(exception.getMessage().contains("already in PUBLISHED status"));
  }

  @Test
  void should_throw_exception_when_updating_canceled_demand() {
    val demand = createTestDemand(PostStatus.CANCELED);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> demandService.updateDemandStatus(demand.getId(), PostStatus.PUBLISHED));

    assertTrue(exception.getMessage().contains("Cannot update status of a canceled demand"));
  }

  @Test
  void should_throw_exception_when_suspended_demand_transitions_to_non_published_status() {
    val demand = createTestDemand(PostStatus.SUSPENDED);

    IllegalStateException exception1 =
        assertThrows(
            IllegalStateException.class,
            () -> demandService.updateDemandStatus(demand.getId(), PostStatus.DRAFT));

    assertTrue(exception1.getMessage().contains("can only transition to PUBLISHED status"));

    IllegalStateException exception2 =
        assertThrows(
            IllegalStateException.class,
            () -> demandService.updateDemandStatus(demand.getId(), PostStatus.CANCELED));

    assertTrue(exception2.getMessage().contains("can only transition to PUBLISHED status"));
  }

  @Test
  void should_throw_exception_when_demand_not_found() {
    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> demandService.updateDemandStatus("non-existent-id", PostStatus.PUBLISHED));

    assertTrue(exception.getMessage().contains("not found or access denied"));
  }

  @Test
  void should_throw_exception_when_demand_belongs_to_different_researcher() {
    val otherResearcher = createOtherResearcher();
    val demand = createDemandForResearcher(otherResearcher, PostStatus.DRAFT);

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> demandService.updateDemandStatus(demand.getId(), PostStatus.PUBLISHED));

    assertTrue(exception.getMessage().contains("not found or access denied"));
  }

  @Test
  void should_get_researcher_demands_by_status() {
    createTestDemand(PostStatus.DRAFT);
    createTestDemand(PostStatus.PUBLISHED);
    createTestDemand(PostStatus.DRAFT);

    Page<Demand> draftDemands =
        demandService.getResearcherDemandsByStatus(PostStatus.DRAFT, DEFAULT_PAGE, DEFAULT_SIZE);

    assertEquals(2, draftDemands.getTotalElements());
    assertEquals(2, draftDemands.getContent().size());
    draftDemands
        .getContent()
        .forEach(
            demand -> {
              assertEquals(PostStatus.DRAFT, demand.getStatus());
              assertEquals(TEST_RESEARCHER_ID, demand.getResearcher().getId());
              assertNotNull(demand.getId());
              assertNotNull(demand.getDescription());
            });
  }

  @Test
  void should_get_all_researcher_demands() {
    createTestDemand(PostStatus.DRAFT);
    createTestDemand(PostStatus.PUBLISHED);
    createTestDemand(PostStatus.CANCELED);

    Page<Demand> allDemands = demandService.getAllResearcherDemands(DEFAULT_PAGE, DEFAULT_SIZE);

    assertEquals(3, allDemands.getTotalElements());
    assertEquals(3, allDemands.getContent().size());
    allDemands
        .getContent()
        .forEach(
            demand -> {
              assertEquals(TEST_RESEARCHER_ID, demand.getResearcher().getId());
              assertNotNull(demand.getId());
              assertNotNull(demand.getStatus());
            });
  }

  @Test
  void should_not_return_other_researcher_demands() {
    val otherResearcher = createOtherResearcher();
    createTestDemand(PostStatus.PUBLISHED);
    createDemandForResearcher(otherResearcher, PostStatus.PUBLISHED);

    Page<Demand> myDemands = demandService.getAllResearcherDemands(DEFAULT_PAGE, DEFAULT_SIZE);

    assertEquals(1, myDemands.getTotalElements());
    assertEquals(TEST_RESEARCHER_ID, myDemands.getContent().getFirst().getResearcher().getId());
  }

  @Test
  void should_get_demand_by_id() {
    val createdDemand = createTestDemand(PostStatus.PUBLISHED);

    val demand = demandService.getDemandById(createdDemand.getId());

    assertNotNull(demand);
    assertEquals(createdDemand.getId(), demand.getId());
    assertEquals(TEST_DESCRIPTION, demand.getDescription());
    assertEquals(TEST_RESEARCHER_ID, demand.getResearcher().getId());
    assertEquals(PostStatus.PUBLISHED, demand.getStatus());
    assertNotNull(demand.getPart());
    assertEquals(TEST_PART_NAME, demand.getPart().getName());
    assertEquals(TEST_CAR_BRAND, demand.getPart().getCarBrand());
    assertEquals(TEST_CAR_MODEL, demand.getPart().getCarModel());
  }

  @Test
  void should_throw_exception_when_getting_non_existent_demand() {
    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class, () -> demandService.getDemandById("non-existent-id"));

    assertTrue(exception.getMessage().contains("not found or access denied"));
  }

  @Test
  void should_throw_exception_when_getting_other_researcher_demand() {
    val otherResearcher = createOtherResearcher();
    val otherDemand = createDemandForResearcher(otherResearcher, PostStatus.PUBLISHED);

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> demandService.getDemandById(otherDemand.getId()));

    assertTrue(exception.getMessage().contains("not found or access denied"));
  }

  @Test
  void should_return_empty_page_when_no_demands_match_status() {
    createTestDemand(PostStatus.DRAFT);

    Page<Demand> publishedDemands =
        demandService.getResearcherDemandsByStatus(
            PostStatus.PUBLISHED, DEFAULT_PAGE, DEFAULT_SIZE);

    assertEquals(0, publishedDemands.getTotalElements());
    assertTrue(publishedDemands.getContent().isEmpty());
  }

  @Test
  void should_handle_pagination_correctly() {
    for (int i = 0; i < 25; i++) {
      createTestDemand(PostStatus.PUBLISHED);
    }

    Page<Demand> firstPageResult =
        demandService.getResearcherDemandsByStatus(
            PostStatus.PUBLISHED, DEFAULT_PAGE, DEFAULT_SIZE);

    assertEquals(DEFAULT_SIZE, firstPageResult.getContent().size());
    assertEquals(25, firstPageResult.getTotalElements());
    assertEquals(3, firstPageResult.getTotalPages());
    assertTrue(firstPageResult.isFirst());
    assertFalse(firstPageResult.isLast());

    Page<Demand> lastPageResult =
        demandService.getResearcherDemandsByStatus(PostStatus.PUBLISHED, 2, DEFAULT_SIZE);

    assertEquals(5, lastPageResult.getContent().size());
    assertTrue(lastPageResult.isLast());
    assertFalse(lastPageResult.isFirst());
  }

  @Test
  void should_get_offers_for_demand() {
    val demand = createTestDemand(PostStatus.PUBLISHED);
    val seller1 = createTestSeller("seller-1");
    val seller2 = createTestSeller("seller-2");

    createPersistedOffer(seller1, demand);
    createPersistedOffer(seller2, demand);

    Page<Offer> offers =
        demandService.getOffersForDemand(demand.getId(), DEFAULT_PAGE, DEFAULT_SIZE);

    assertEquals(2, offers.getTotalElements());
    assertEquals(2, offers.getContent().size());
    offers
        .getContent()
        .forEach(
            offer -> {
              assertEquals(demand.getId(), offer.getDemand().getId());
              assertNotNull(offer.getPartsInfo());
              assertNotNull(offer.getId());
              assertNotNull(offer.getSellerId());
            });
  }

  @Test
  void should_return_empty_page_when_no_offers_for_demand() {
    val demand = createTestDemand(PostStatus.PUBLISHED);

    Page<Offer> offers =
        demandService.getOffersForDemand(demand.getId(), DEFAULT_PAGE, DEFAULT_SIZE);

    assertEquals(0, offers.getTotalElements());
    assertTrue(offers.getContent().isEmpty());
  }

  @Test
  void should_throw_exception_when_getting_offers_for_other_researcher_demand() {
    val otherResearcher = createOtherResearcher();
    val otherDemand = createDemandForResearcher(otherResearcher, PostStatus.PUBLISHED);

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () ->
                demandService.getOffersForDemand(otherDemand.getId(), DEFAULT_PAGE, DEFAULT_SIZE));

    assertTrue(exception.getMessage().contains("not found or access denied"));
  }

  @Test
  void should_throw_exception_when_getting_offers_for_non_existent_demand() {
    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> demandService.getOffersForDemand("non-existent-id", DEFAULT_PAGE, DEFAULT_SIZE));

    assertTrue(exception.getMessage().contains("not found or access denied"));
  }

  @Test
  void should_notify_all_sellers_when_demand_is_published() throws InterruptedException {
    val seller1 = createTestSeller("seller-a");
    val seller2 = createTestSeller("seller-b");
    val seller3 = createTestSeller("seller-c");

    val restPart = buildTestRestPart();
    val createdDemand = demandService.createDemand(TEST_DESCRIPTION, restPart);

    assertNotNull(createdDemand.getId());
    assertEquals(TEST_DESCRIPTION, createdDemand.getDescription());
    assertEquals(PostStatus.DRAFT, createdDemand.getStatus());
    assertEquals(TEST_RESEARCHER_ID, createdDemand.getResearcher().getId());
    assertEquals(TEST_RESEARCHER_NAME, createdDemand.getResearcher().getName());
    assertEquals(TEST_EMAIL, createdDemand.getResearcher().getEmail());
    assertEquals(TEST_PART_NAME, createdDemand.getPart().getName());
    assertEquals(TEST_CAR_BRAND, createdDemand.getPart().getCarBrand());
    assertEquals(TEST_CAR_MODEL, createdDemand.getPart().getCarModel());
    assertEquals(Year.of(TEST_CAR_YEAR), createdDemand.getPart().getCarYear());
    assertEquals(PartCategory.FOG_LIGHTS, createdDemand.getPart().getPartCategory());
    assertNotNull(createdDemand.getCreatedAt());
    assertNotNull(createdDemand.getUpdatedAt());
    assertNull(createdDemand.getCanceledAt());
    assertNull(createdDemand.getSuspendedAt());

    val savedDraft = demandRepository.findById(createdDemand.getId());
    assertTrue(savedDraft.isPresent());
    assertEquals(PostStatus.DRAFT, savedDraft.get().getStatus());
    assertEquals(TEST_DESCRIPTION, savedDraft.get().getDescription());
    assertEquals(TEST_RESEARCHER_ID, savedDraft.get().getResearcher().getId());
    assertNotNull(savedDraft.get().getPart());
    assertEquals(TEST_PART_NAME, savedDraft.get().getPart().getPartName());
    assertEquals(TEST_CAR_BRAND, savedDraft.get().getPart().getCarBrand());
    assertEquals(TEST_CAR_MODEL, savedDraft.get().getPart().getCarModel());
    assertEquals(TEST_CAR_YEAR, savedDraft.get().getPart().getCarYear());
    assertEquals(PartCategory.FOG_LIGHTS, savedDraft.get().getPart().getPartCategory());
    assertNull(savedDraft.get().getCanceledAt());
    assertNull(savedDraft.get().getSuspendedAt());

    val updatedAtBeforePublish = createdDemand.getUpdatedAt();
    val publishedDemand =
        demandService.updateDemandStatus(createdDemand.getId(), PostStatus.PUBLISHED);

    assertEquals(createdDemand.getId(), publishedDemand.getId());
    assertEquals(PostStatus.PUBLISHED, publishedDemand.getStatus());
    assertNull(publishedDemand.getCanceledAt());
    assertNull(publishedDemand.getSuspendedAt());
    assertTrue(
        publishedDemand.getUpdatedAt().isAfter(updatedAtBeforePublish)
            || publishedDemand.getUpdatedAt().equals(updatedAtBeforePublish));

    val savedPublished = demandRepository.findById(createdDemand.getId());
    assertTrue(savedPublished.isPresent());
    assertEquals(PostStatus.PUBLISHED, savedPublished.get().getStatus());
    assertNull(savedPublished.get().getCanceledAt());
    assertNull(savedPublished.get().getSuspendedAt());
    assertNotNull(savedPublished.get().getUpdatedAt());

    Thread.sleep(5000);

    val notifications = notificationRepository.findAll();
    assertEquals(3, notifications.size());

    val notifiedSellerIds =
        notifications.stream().map(n -> n.getSeller().getId()).collect(Collectors.toSet());
    assertTrue(notifiedSellerIds.contains(seller1.getId()));
    assertTrue(notifiedSellerIds.contains(seller2.getId()));
    assertTrue(notifiedSellerIds.contains(seller3.getId()));

    notifications.forEach(
        notification -> {
          assertEquals(createdDemand.getId(), notification.getDemand().getId());
          assertEquals(NotificationType.DEMAND_PUBLISHED, notification.getNotificationType());
          assertFalse(notification.isRead());
          assertNull(notification.getReadAt());
          assertNotNull(notification.getCreatedAt());
          assertNotNull(notification.getId());
          assertNotNull(notification.getNotificationRequested());
          assertNotNull(notification.getNotificationRequested().getId());
          assertNotNull(notification.getMessage());
          assertFalse(notification.getMessage().isBlank());
        });

    val eventLogs = demandPublishedRequestedRepository.findAll();
    assertEquals(1, eventLogs.size());
    val eventLog = eventLogs.getFirst();
    assertEquals(createdDemand.getId(), eventLog.getDemand().getId());
    assertEquals(ProcessStatus.SUCCESS, eventLog.getStatus());
    assertEquals(3, eventLog.getTotalSellersToNotify());
    assertEquals(3, eventLog.getNotificationsSentCount());
    assertNotNull(eventLog.getCompletedAt());
    assertNull(eventLog.getErrorMessage());

    val notificationRequestedLogs = notificationRequestedRepository.findAll();
    assertEquals(3, notificationRequestedLogs.size());

    val notificationRequestedSellerIds =
        notificationRequestedLogs.stream()
            .map(log -> log.getSeller().getId())
            .collect(Collectors.toSet());
    assertTrue(notificationRequestedSellerIds.contains(seller1.getId()));
    assertTrue(notificationRequestedSellerIds.contains(seller2.getId()));
    assertTrue(notificationRequestedSellerIds.contains(seller3.getId()));

    notificationRequestedLogs.forEach(
        log -> {
          assertEquals(eventLog.getId(), log.getDemandPublishedRequested().getId());
          assertEquals(createdDemand.getId(), log.getDemand().getId());
        });
  }

  private void authenticateResearcher() {
    val authentication =
        new UsernamePasswordAuthenticationToken(TEST_SUPABASE_USER_ID, null, new ArrayList<>());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private RestPart buildTestRestPart() {
    return new RestPart(
        TEST_PART_NAME,
        TEST_CAR_BRAND,
        TEST_CAR_MODEL,
        Year.of(TEST_CAR_YEAR),
        new ArrayList<>(),
        PartCategory.FOG_LIGHTS);
  }

  private JResearcher createTestResearcher() {
    return userRepository.save(
        JResearcher.builder()
            .id(TEST_RESEARCHER_ID)
            .supabaseUserId(TEST_SUPABASE_USER_ID)
            .name(TEST_RESEARCHER_NAME)
            .phoneNumber(TEST_PHONE)
            .email(TEST_EMAIL)
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JResearcher createOtherResearcher() {
    return userRepository.save(
        JResearcher.builder()
            .id("other-researcher-" + currentTimeMillis())
            .supabaseUserId("other-supabase-" + currentTimeMillis())
            .name("Jane Doe")
            .email("jane-" + currentTimeMillis() + "@gmail.com")
            .phoneNumber("+9876543210")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JSeller createTestSeller(String id) {
    return userRepository.save(
        JSeller.builder()
            .id(id + "-" + currentTimeMillis())
            .supabaseUserId("supabase-" + id + "-" + currentTimeMillis())
            .name("Seller " + id)
            .email(id + "-" + currentTimeMillis() + "@gmail.com")
            .phoneNumber("+1234567890")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JDemand createTestDemand(PostStatus status) {
    return createDemandForResearcher(testResearcher, status);
  }

  private JDemand createDemandForResearcher(JResearcher researcher, PostStatus status) {
    val part =
        JPart.builder()
            .id("part-" + currentTimeMillis())
            .partName(TEST_PART_NAME)
            .carBrand(TEST_CAR_BRAND)
            .carModel(TEST_CAR_MODEL)
            .carYear(TEST_CAR_YEAR)
            .partCategory(PartCategory.FOG_LIGHTS)
            .imageBuckets(new ArrayList<>())
            .build();

    val demand =
        JDemand.builder()
            .id("demand-" + currentTimeMillis())
            .description(TEST_DESCRIPTION)
            .attachedPhotoBucketKeys(new ArrayList<>())
            .part(part)
            .researcher(researcher)
            .status(status)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    if (status == PostStatus.CANCELED) demand.setCanceledAt(LocalDateTime.now());
    if (status == PostStatus.SUSPENDED) demand.setSuspendedAt(LocalDateTime.now());

    part.setDemand(demand);
    return demandRepository.save(demand);
  }

  private void createPersistedOffer(JSeller seller, JDemand demand) {
    val partInfo =
        JPartInfo.builder()
            .id("part-info-" + currentTimeMillis())
            .partName(TEST_PART_NAME)
            .carBrand(TEST_CAR_BRAND)
            .carModel(TEST_CAR_MODEL)
            .carYear(TEST_CAR_YEAR)
            .partCategory(PartCategory.FOG_LIGHTS)
            .condition(PartCondition.USED)
            .price(TEST_PRICE)
            .partImageBuckets(new ArrayList<>())
            .build();

    val offer =
        JOffer.builder()
            .id("offer-" + currentTimeMillis())
            .demand(demand)
            .description("Offering " + TEST_PART_NAME)
            .attachedPhotoBucketKeys(new ArrayList<>())
            .partInfo(partInfo)
            .seller(seller)
            .status(PostStatus.PUBLISHED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    partInfo.setOffer(offer);
    offerRepository.save(offer);
  }
}
