package com.devikapps.vaikaparts.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange.RestPart;
import com.devikapps.vaikaparts.event.model.DemandPublishedRequested;
import com.devikapps.vaikaparts.event.model.EventProducer;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.file.BucketComponent;
import com.devikapps.vaikaparts.file.FilenameSanitizer;
import com.devikapps.vaikaparts.mapper.exchange.DemandMapper;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Part;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.service.util.ImageUploader;
import com.devikapps.vaikaparts.service.util.Paginator;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class DemandServiceTest {

  private static final String TEST_DEMAND_ID = "demand-123";
  private static final String TEST_RESEARCHER_ID = "researcher-456";
  private static final String TEST_DESCRIPTION = "Looking for a headlight for Toyota Corolla 2015";
  private static final String TEST_PART_NAME = "Headlight";
  private static final String TEST_CAR_BRAND = "Toyota";
  private static final String TEST_CAR_MODEL = "Corolla";
  private static final int TEST_CAR_YEAR = 2015;
  private static final Integer DEFAULT_PAGE = 0;
  private static final Integer DEFAULT_SIZE = 10;

  @Mock private DemandRepository demandRepository;
  @Mock private DemandMapper demandMapper;
  @Mock private ResearcherService researcherService;
  @Mock private BucketComponent bucketComponent;
  @Mock private Paginator paginator;
  @Mock private FilenameSanitizer filenameSanitizer;
  @Mock private ImageUploader imageUploader;
  @Mock private EventProducer<DemandPublishedRequested> demandPublishedRequestedProducer;

  @InjectMocks private DemandService demandService;

  private Researcher testResearcher;
  private RestPart testRestPart;
  private Demand testDemand;
  private JDemand testJDemand;
  private JResearcher testJResearcher;
  private JPart testJPart;

  @BeforeEach
  void setUp() {
    testResearcher = buildTestResearcher();
    testRestPart = buildTestRestPart();
    testDemand = buildTestDemand();
    testJResearcher = buildTestJResearcher();
    testJPart = buildTestJPart();
    testJDemand = buildTestJDemand();
  }

  @Test
  void should_create_demand_successfully() throws Exception {
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandMapper.toPersistence(any(Demand.class))).thenReturn(testJDemand);
    when(demandRepository.save(any(JDemand.class))).thenReturn(testJDemand);
    when(demandMapper.toDomain(testJDemand)).thenReturn(testDemand);

    Demand result = demandService.createDemand(TEST_DESCRIPTION, testRestPart);

    assertNotNull(result);
    assertEquals(TEST_DESCRIPTION, result.getDescription());
    assertEquals(PostStatus.DRAFT, result.getStatus());
    assertEquals(TEST_RESEARCHER_ID, result.getResearcher().getId());
    verify(demandRepository, times(1)).save(any(JDemand.class));
    verify(researcherService, times(1)).getCurrentResearcher();
  }

  @Test
  void should_throw_exception_when_description_is_null() {
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);

    assertThrows(
        IllegalArgumentException.class, () -> demandService.createDemand(null, testRestPart));

    verify(demandRepository, never()).save(any(JDemand.class));
  }

  @Test
  void should_throw_exception_when_description_is_blank() {
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);

    assertThrows(
        IllegalArgumentException.class, () -> demandService.createDemand("   ", testRestPart));

    verify(demandRepository, never()).save(any(JDemand.class));
  }

  @Test
  void should_throw_exception_when_part_is_null() {
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);

    assertThrows(
        IllegalArgumentException.class, () -> demandService.createDemand(TEST_DESCRIPTION, null));

    verify(demandRepository, never()).save(any(JDemand.class));
  }

  @Test
  void should_update_demand_status_to_published() {
    testJDemand.setStatus(PostStatus.DRAFT);

    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testJDemand));
    when(demandRepository.save(testJDemand)).thenReturn(testJDemand);
    when(demandMapper.toDomain(testJDemand)).thenReturn(testDemand);

    Demand result = demandService.updateDemandStatus(TEST_DEMAND_ID, PostStatus.PUBLISHED);

    assertNotNull(result);
    assertEquals(PostStatus.PUBLISHED, testJDemand.getStatus());
    assertNull(testJDemand.getCanceledAt());
    assertNull(testJDemand.getSuspendedAt());
    verify(demandRepository, times(1)).save(testJDemand);
    verify(demandPublishedRequestedProducer, times(1)).accept(anyList());
  }

  @Test
  void should_publish_event_when_demand_first_published() {
    testJDemand.setStatus(PostStatus.DRAFT);

    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testJDemand));
    when(demandRepository.save(testJDemand)).thenReturn(testJDemand);
    when(demandMapper.toDomain(testJDemand)).thenReturn(testDemand);

    demandService.updateDemandStatus(TEST_DEMAND_ID, PostStatus.PUBLISHED);

    ArgumentCaptor<List<DemandPublishedRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(demandPublishedRequestedProducer, times(1)).accept(captor.capture());

    List<DemandPublishedRequested> events = captor.getValue();
    assertEquals(1, events.size());
    assertEquals(TEST_DEMAND_ID, events.getFirst().getDemandId());
    assertNotNull(events.getFirst().getId());
  }

  @Test
  void should_not_publish_event_when_notifications_already_sent() {
    testJDemand.setStatus(PostStatus.SUSPENDED);

    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testJDemand));
    when(demandRepository.save(testJDemand)).thenReturn(testJDemand);
    when(demandMapper.toDomain(testJDemand)).thenReturn(testDemand);

    demandService.updateDemandStatus(TEST_DEMAND_ID, PostStatus.PUBLISHED);
  }

  @Test
  void should_not_publish_event_when_transitioning_to_non_published_status() {
    testJDemand.setStatus(PostStatus.DRAFT);

    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testJDemand));
    when(demandRepository.save(testJDemand)).thenReturn(testJDemand);
    when(demandMapper.toDomain(testJDemand)).thenReturn(testDemand);

    demandService.updateDemandStatus(TEST_DEMAND_ID, PostStatus.SUSPENDED);

    verify(demandPublishedRequestedProducer, never()).accept(anyList());
  }

  @Test
  void should_update_demand_status_to_canceled() {
    testJDemand.setStatus(PostStatus.PUBLISHED);
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testJDemand));
    when(demandRepository.save(testJDemand)).thenReturn(testJDemand);
    when(demandMapper.toDomain(testJDemand)).thenReturn(testDemand);

    Demand result = demandService.updateDemandStatus(TEST_DEMAND_ID, PostStatus.CANCELED);

    assertNotNull(result);
    assertEquals(PostStatus.CANCELED, testJDemand.getStatus());
    assertNotNull(testJDemand.getCanceledAt());
    verify(demandRepository, times(1)).save(testJDemand);
  }

  @Test
  void should_update_demand_status_to_suspended() {
    testJDemand.setStatus(PostStatus.PUBLISHED);
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testJDemand));
    when(demandRepository.save(testJDemand)).thenReturn(testJDemand);
    when(demandMapper.toDomain(testJDemand)).thenReturn(testDemand);

    Demand result = demandService.updateDemandStatus(TEST_DEMAND_ID, PostStatus.SUSPENDED);

    assertNotNull(result);
    assertEquals(PostStatus.SUSPENDED, testJDemand.getStatus());
    assertNotNull(testJDemand.getSuspendedAt());
    verify(demandRepository, times(1)).save(testJDemand);
  }

  @Test
  void should_throw_exception_when_updating_to_same_status() {
    testJDemand.setStatus(PostStatus.PUBLISHED);
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testJDemand));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> demandService.updateDemandStatus(TEST_DEMAND_ID, PostStatus.PUBLISHED));

    assertTrue(exception.getMessage().contains("already in PUBLISHED status"));
    verify(demandRepository, never()).save(any(JDemand.class));
  }

  @Test
  void should_throw_exception_when_updating_canceled_demand() {
    testJDemand.setStatus(PostStatus.CANCELED);
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testJDemand));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> demandService.updateDemandStatus(TEST_DEMAND_ID, PostStatus.PUBLISHED));

    assertTrue(exception.getMessage().contains("Cannot update status of a canceled demand"));
    verify(demandRepository, never()).save(any(JDemand.class));
  }

  @Test
  void should_throw_exception_when_suspended_demand_transitions_to_non_published_status() {
    testJDemand.setStatus(PostStatus.SUSPENDED);
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testJDemand));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> demandService.updateDemandStatus(TEST_DEMAND_ID, PostStatus.DRAFT));

    assertTrue(exception.getMessage().contains("can only transition to PUBLISHED status"));
    verify(demandRepository, never()).save(any(JDemand.class));
  }

  @Test
  void should_throw_exception_when_demand_not_found() {
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID)).thenReturn(Optional.empty());

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> demandService.updateDemandStatus(TEST_DEMAND_ID, PostStatus.PUBLISHED));

    assertTrue(exception.getMessage().contains("not found or access denied"));
  }

  @Test
  void should_throw_exception_when_demand_belongs_to_different_researcher() {
    JResearcher differentResearcher = JResearcher.builder().id("different-researcher-id").build();
    testJDemand.setResearcher(differentResearcher);

    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testJDemand));

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> demandService.updateDemandStatus(TEST_DEMAND_ID, PostStatus.PUBLISHED));

    assertTrue(exception.getMessage().contains("not found or access denied"));
  }

  @Test
  void should_get_researcher_demands_by_status() {
    var pageable = PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE, Sort.by("createdAt").descending());
    Page<JDemand> jDemandPage = new PageImpl<>(List.of(testJDemand));

    when(paginator.apply(DEFAULT_PAGE, DEFAULT_SIZE))
        .thenReturn(Map.of("page", DEFAULT_PAGE, "size", DEFAULT_SIZE));
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByResearcherIdAndStatus(
            TEST_RESEARCHER_ID, PostStatus.PUBLISHED, pageable))
        .thenReturn(jDemandPage);
    when(demandMapper.toDomain(testJDemand)).thenReturn(testDemand);

    Page<Demand> result =
        demandService.getResearcherDemandsByStatus(
            PostStatus.PUBLISHED, DEFAULT_PAGE, DEFAULT_SIZE);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(testDemand, result.getContent().getFirst());
    assertEquals(TEST_DEMAND_ID, result.getContent().getFirst().getId());
    verify(demandRepository, times(1))
        .findByResearcherIdAndStatus(TEST_RESEARCHER_ID, PostStatus.PUBLISHED, pageable);
  }

  @Test
  void should_get_all_researcher_demands() {
    var pageable = PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE, Sort.by("createdAt").descending());
    Page<JDemand> jDemandPage = new PageImpl<>(List.of(testJDemand));

    when(paginator.apply(DEFAULT_PAGE, DEFAULT_SIZE))
        .thenReturn(Map.of("page", DEFAULT_PAGE, "size", DEFAULT_SIZE));
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByResearcherIdWithRelations(TEST_RESEARCHER_ID, pageable))
        .thenReturn(jDemandPage);
    when(demandMapper.toDomain(testJDemand)).thenReturn(testDemand);

    Page<Demand> result = demandService.getAllResearcherDemands(DEFAULT_PAGE, DEFAULT_SIZE);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals(testDemand, result.getContent().getFirst());
    verify(demandRepository, times(1))
        .findByResearcherIdWithRelations(TEST_RESEARCHER_ID, pageable);
  }

  @Test
  void should_get_demand_by_id() {
    when(researcherService.getCurrentResearcher()).thenReturn(testResearcher);
    when(demandRepository.findByIdWithRelations(TEST_DEMAND_ID))
        .thenReturn(Optional.of(testJDemand));
    when(demandMapper.toDomain(testJDemand)).thenReturn(testDemand);

    Demand result = demandService.getDemandById(TEST_DEMAND_ID);

    assertNotNull(result);
    assertEquals(TEST_DEMAND_ID, result.getId());
    assertEquals(TEST_DESCRIPTION, result.getDescription());
    verify(demandRepository, times(1)).findByIdWithRelations(TEST_DEMAND_ID);
  }

  private Researcher buildTestResearcher() {
    return Researcher.builder().id(TEST_RESEARCHER_ID).name("John Doe").build();
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

  private Demand buildTestDemand() {
    var part =
        Part.builder()
            .id("part-123")
            .name(TEST_PART_NAME)
            .carBrand(TEST_CAR_BRAND)
            .carModel(TEST_CAR_MODEL)
            .carYear(Year.of(TEST_CAR_YEAR))
            .imageUrls(List.of())
            .partCategory(PartCategory.FOG_LIGHTS)
            .build();

    return Demand.builder()
        .id(TEST_DEMAND_ID)
        .description(TEST_DESCRIPTION)
        .part(part)
        .researcher(testResearcher)
        .status(PostStatus.DRAFT)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  private JResearcher buildTestJResearcher() {
    return JResearcher.builder().id(TEST_RESEARCHER_ID).name("John Doe").build();
  }

  private JPart buildTestJPart() {
    return JPart.builder()
        .id("part-123")
        .partName(TEST_PART_NAME)
        .carBrand(TEST_CAR_BRAND)
        .carModel(TEST_CAR_MODEL)
        .carYear(TEST_CAR_YEAR)
        .partCategory(PartCategory.FOG_LIGHTS)
        .imageBuckets(List.of())
        .build();
  }

  private JDemand buildTestJDemand() {
    return JDemand.builder()
        .id(TEST_DEMAND_ID)
        .description(TEST_DESCRIPTION)
        .part(testJPart)
        .researcher(testJResearcher)
        .status(PostStatus.DRAFT)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }
}
