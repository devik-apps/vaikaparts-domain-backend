package com.devikapps.vaikaparts.service;

import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.RestPart;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
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

  private static final Integer DEFAULT_PAGE = 0;
  private static final Integer DEFAULT_SIZE = 10;

  @Autowired private DemandService demandService;
  @Autowired private ResearcherService researcherService;
  @Autowired private DemandRepository demandRepository;
  @Autowired private ValueObjectMapper vom;
  @Autowired private UserRepository userRepository;

  private JResearcher testResearcher;

  @BeforeEach
  void setUp() {
    testResearcher = createTestResearcher();
    authenticateResearcher();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
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
    assertEquals(TEST_PART_NAME, demand.getPart().getName());
    assertNotNull(demand.getCreatedAt());
    assertNotNull(demand.getUpdatedAt());
    assertNull(demand.getCanceledAt());
    assertNull(demand.getSuspendedAt());

    val savedDemand = demandRepository.findById(demand.getId());
    assertTrue(savedDemand.isPresent());
    assertEquals(TEST_DESCRIPTION, savedDemand.get().getDescription());
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
            "image1", "headlight-front.jpg", "image/jpeg", "fake image content 1".getBytes());

    val imageFile2 =
        new MockMultipartFile(
            "image2", "headlight-side.jpg", "image/jpeg", "fake image content 2".getBytes());

    val imageFile3 =
        new MockMultipartFile(
            "image3", "headlight-back.jpg", "image/jpeg", "fake image content 3".getBytes());

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
    assertNotNull(savedDemand.get().getPart().getImageBuckets());
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
    assertNotNull(savedDemand.get().getPart().getImageBuckets());
    assertTrue(savedDemand.get().getPart().getImageBuckets().isEmpty());
  }

  @Test
  void should_throw_exception_when_description_is_null() {
    val restPart = buildTestRestPart();

    assertThrows(IllegalArgumentException.class, () -> demandService.createDemand(null, restPart));
  }

  @Test
  void should_throw_exception_when_description_is_blank() {
    val restPart = buildTestRestPart();

    assertThrows(IllegalArgumentException.class, () -> demandService.createDemand("   ", restPart));
  }

  @Test
  void should_throw_exception_when_part_is_null() {
    assertThrows(
        IllegalArgumentException.class, () -> demandService.createDemand(TEST_DESCRIPTION, null));
  }

  @Test
  void should_update_demand_status_to_published() {
    val demand = createTestDemand(PostStatus.DRAFT);

    val updatedDemand = demandService.updateDemandStatus(demand.getId(), PostStatus.PUBLISHED);

    assertNotNull(updatedDemand);
    assertEquals(PostStatus.PUBLISHED, updatedDemand.getStatus());
    assertNull(updatedDemand.getCanceledAt());
    assertNull(updatedDemand.getSuspendedAt());
    assertNotNull(updatedDemand.getUpdatedAt());

    val savedDemand = demandRepository.findById(demand.getId());
    assertTrue(savedDemand.isPresent());
    assertEquals(PostStatus.PUBLISHED, savedDemand.get().getStatus());
  }

  @Test
  void should_update_demand_status_to_canceled() {
    val demand = createTestDemand(PostStatus.PUBLISHED);

    val updatedDemand = demandService.updateDemandStatus(demand.getId(), PostStatus.CANCELED);

    assertNotNull(updatedDemand);
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

    assertNotNull(updatedDemand);
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

    assertNotNull(updatedDemand);
    assertEquals(PostStatus.PUBLISHED, updatedDemand.getStatus());
    assertNull(updatedDemand.getSuspendedAt());
    assertNull(updatedDemand.getCanceledAt());
  }

  @Test
  void should_throw_exception_when_updating_to_same_status() {
    val demand = createTestDemand(PostStatus.PUBLISHED);

    assertThrows(
        IllegalStateException.class,
        () -> demandService.updateDemandStatus(demand.getId(), PostStatus.PUBLISHED));
  }

  @Test
  void should_throw_exception_when_updating_canceled_demand() {
    val demand = createTestDemand(PostStatus.CANCELED);

    assertThrows(
        IllegalStateException.class,
        () -> demandService.updateDemandStatus(demand.getId(), PostStatus.PUBLISHED));
  }

  @Test
  void should_throw_exception_when_suspended_demand_transitions_to_non_published_status() {
    val demand = createTestDemand(PostStatus.SUSPENDED);

    assertThrows(
        IllegalStateException.class,
        () -> demandService.updateDemandStatus(demand.getId(), PostStatus.DRAFT));

    assertThrows(
        IllegalStateException.class,
        () -> demandService.updateDemandStatus(demand.getId(), PostStatus.CANCELED));
  }

  @Test
  void should_throw_exception_when_demand_not_found() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> demandService.updateDemandStatus("non-existent-id", PostStatus.PUBLISHED));
  }

  @Test
  void should_throw_exception_when_demand_belongs_to_different_researcher() {
    val otherResearcher = createOtherResearcher();
    val demand = createDemandForResearcher(otherResearcher, PostStatus.DRAFT);

    assertThrows(
        ResourceNotFoundException.class,
        () -> demandService.updateDemandStatus(demand.getId(), PostStatus.PUBLISHED));
  }

  @Test
  void should_get_researcher_demands_by_status() {
    createTestDemand(PostStatus.DRAFT);
    createTestDemand(PostStatus.PUBLISHED);
    createTestDemand(PostStatus.DRAFT);

    Page<Demand> draftDemands =
        demandService.getResearcherDemandsByStatus(PostStatus.DRAFT, DEFAULT_PAGE, DEFAULT_SIZE);

    assertNotNull(draftDemands);
    assertEquals(2, draftDemands.getTotalElements());
    draftDemands
        .getContent()
        .forEach(
            demand -> {
              assertEquals(PostStatus.DRAFT, demand.getStatus());
              assertEquals(TEST_RESEARCHER_ID, demand.getResearcher().getId());
            });
  }

  @Test
  void should_get_all_researcher_demands() {
    createTestDemand(PostStatus.DRAFT);
    createTestDemand(PostStatus.PUBLISHED);
    createTestDemand(PostStatus.CANCELED);

    Page<Demand> allDemands = demandService.getAllResearcherDemands(DEFAULT_PAGE, DEFAULT_SIZE);

    assertNotNull(allDemands);
    assertEquals(3, allDemands.getTotalElements());
    allDemands
        .getContent()
        .forEach(demand -> assertEquals(TEST_RESEARCHER_ID, demand.getResearcher().getId()));
  }

  @Test
  void should_not_return_other_researcher_demands() {
    val otherResearcher = createOtherResearcher();
    createTestDemand(PostStatus.PUBLISHED);
    createDemandForResearcher(otherResearcher, PostStatus.PUBLISHED);

    Page<Demand> myDemands = demandService.getAllResearcherDemands(DEFAULT_PAGE, DEFAULT_SIZE);

    assertNotNull(myDemands);
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
    assertNotNull(demand.getPart());
  }

  @Test
  void should_throw_exception_when_getting_non_existent_demand() {
    assertThrows(
        ResourceNotFoundException.class, () -> demandService.getDemandById("non-existent-id"));
  }

  @Test
  void should_throw_exception_when_getting_other_researcher_demand() {
    val otherResearcher = createOtherResearcher();
    val otherDemand = createDemandForResearcher(otherResearcher, PostStatus.PUBLISHED);

    assertThrows(
        ResourceNotFoundException.class, () -> demandService.getDemandById(otherDemand.getId()));
  }

  @Test
  void should_return_empty_page_when_no_demands_match_status() {
    createTestDemand(PostStatus.DRAFT);

    Page<Demand> publishedDemands =
        demandService.getResearcherDemandsByStatus(
            PostStatus.PUBLISHED, DEFAULT_PAGE, DEFAULT_SIZE);

    assertNotNull(publishedDemands);
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

  private JResearcher createTestResearcher() {
    val researcher =
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
            .build();
    return userRepository.save(researcher);
  }

  private JResearcher createOtherResearcher() {
    val researcher =
        JResearcher.builder()
            .id("other-researcher-456")
            .supabaseUserId("other-supabase-user-456")
            .name("Jane Doe")
            .email("jane@gmail.com")
            .phoneNumber("+9876543210")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build();
    return userRepository.save(researcher);
  }

  @SneakyThrows
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
            .imageBuckets(List.of())
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
    else if (status == PostStatus.SUSPENDED) demand.setSuspendedAt(LocalDateTime.now());

    part.setDemand(demand);
    return demandRepository.save(demand);
  }
}
