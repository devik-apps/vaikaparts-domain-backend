package com.devikapps.vaikaparts.service;

import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.exchange.RestPartInfo;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PartCondition;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JOffer;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.exchange.JPartInfo;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

class OfferServiceIT extends FacadeIT {

  private static final String TEST_SELLER_ID = "seller-123";
  private static final String TEST_SELLER_SUPABASE_ID = "supabase-seller-123";
  private static final String TEST_SELLER_NAME = "Jane Seller";
  private static final String TEST_RESEARCHER_ID = "researcher-123";
  private static final String TEST_RESEARCHER_SUPABASE_ID = "supabase-researcher-123";
  private static final String TEST_EMAIL = "seller@gmail.com";
  private static final String TEST_PHONE = "+1234567890";
  private static final String TEST_DESCRIPTION = "Offering a headlight for Toyota Corolla 2015";
  private static final String TEST_PART_NAME = "Headlight";
  private static final String TEST_CAR_BRAND = "Toyota";
  private static final String TEST_CAR_MODEL = "Corolla";
  private static final int TEST_CAR_YEAR = 2015;
  private static final double TEST_PRICE = 150.00;
  private static final Integer DEFAULT_PAGE = 0;
  private static final Integer DEFAULT_SIZE = 10;

  @Autowired private OfferService offerService;
  @Autowired private OfferRepository offerRepository;
  @Autowired private DemandRepository demandRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ValueObjectMapper vom;

  private JSeller testSeller;
  private JDemand testDemand;

  @BeforeEach
  void setUp() {
    testSeller = createTestSeller();
    val researcher = createTestResearcher();
    testDemand = createTestDemand(researcher, PostStatus.PUBLISHED);
    authenticateSeller();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    offerRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void should_create_offer_successfully() {
    val offer =
        offerService.createOffer(testDemand.getId(), TEST_DESCRIPTION, buildTestRestPartInfo());

    assertNotNull(offer);
    assertNotNull(offer.getId());
    assertEquals(TEST_DESCRIPTION, offer.getDescription());
    assertEquals(PostStatus.DRAFT, offer.getStatus());
    assertEquals(TEST_SELLER_ID, offer.getSellerId());
    assertNotNull(offer.getDemand());
    assertEquals(testDemand.getId(), offer.getDemand().getId());
    assertNotNull(offer.getPartsInfo());
    assertNotNull(offer.getPartsInfo().getPart());
    assertEquals(TEST_PART_NAME, offer.getPartsInfo().getPart().getName());
    assertEquals(TEST_PRICE, offer.getPartsInfo().getPrice());
    assertEquals(PartCondition.USED, offer.getPartsInfo().getCondition());
    assertNotNull(offer.getCreatedAt());
    assertNotNull(offer.getUpdatedAt());
    assertNull(offer.getCanceledAt());
    assertNull(offer.getSuspendedAt());

    val saved = offerRepository.findByIdWithRelations(offer.getId());
    assertTrue(saved.isPresent());
    assertEquals(testDemand.getId(), saved.get().getDemand().getId());
  }

  @Test
  void should_create_offer_with_single_image_successfully() {
    val image =
        new MockMultipartFile("image", "part.jpg", "image/jpeg", "fake image content".getBytes());
    val restPartInfo =
        new RestPartInfo(
            TEST_PART_NAME,
            TEST_CAR_BRAND,
            TEST_CAR_MODEL,
            Year.of(TEST_CAR_YEAR),
            PartCategory.FOG_LIGHTS,
            PartCondition.USED,
            BigDecimal.valueOf(TEST_PRICE),
            List.of(image));

    val offer = offerService.createOffer(testDemand.getId(), TEST_DESCRIPTION, restPartInfo);

    assertNotNull(offer);
    assertNotNull(offer.getPartsInfo().getPart().getImageUrls());
    assertEquals(1, offer.getPartsInfo().getPart().getImageUrls().size());

    val saved = offerRepository.findByIdWithRelations(offer.getId());
    assertTrue(saved.isPresent());
    assertEquals(1, saved.get().getPartInfo().getPartImageBuckets().size());
  }

  @Test
  void should_create_offer_with_multiple_images_successfully() {
    List<MultipartFile> images =
        List.of(
            new MockMultipartFile("img1", "front.jpg", "image/jpeg", "c1".getBytes()),
            new MockMultipartFile("img2", "side.jpg", "image/jpeg", "c2".getBytes()),
            new MockMultipartFile("img3", "back.jpg", "image/jpeg", "c3".getBytes()));

    val restPartInfo =
        new RestPartInfo(
            TEST_PART_NAME,
            TEST_CAR_BRAND,
            TEST_CAR_MODEL,
            Year.of(TEST_CAR_YEAR),
            PartCategory.FOG_LIGHTS,
            PartCondition.USED,
            BigDecimal.valueOf(TEST_PRICE),
            images);

    val offer = offerService.createOffer(testDemand.getId(), TEST_DESCRIPTION, restPartInfo);

    assertNotNull(offer);
    assertNotNull(offer.getPartsInfo().getPart().getImageUrls());
    assertEquals(3, offer.getPartsInfo().getPart().getImageUrls().size());

    val saved = offerRepository.findByIdWithRelations(offer.getId());
    assertTrue(saved.isPresent());
    assertEquals(3, saved.get().getPartInfo().getPartImageBuckets().size());
  }

  @Test
  void should_create_offer_without_images_successfully() {
    val offer =
        offerService.createOffer(testDemand.getId(), TEST_DESCRIPTION, buildTestRestPartInfo());

    assertNotNull(offer);
    assertNotNull(offer.getPartsInfo().getPart().getImageUrls());
    assertTrue(offer.getPartsInfo().getPart().getImageUrls().isEmpty());

    val saved = offerRepository.findByIdWithRelations(offer.getId());
    assertTrue(saved.isPresent());
    assertTrue(saved.get().getPartInfo().getPartImageBuckets().isEmpty());
  }

  @Test
  void should_throw_exception_when_description_is_null() {
    assertThrows(
        IllegalArgumentException.class,
        () -> offerService.createOffer(testDemand.getId(), null, buildTestRestPartInfo()));
  }

  @Test
  void should_throw_exception_when_description_is_blank() {
    assertThrows(
        IllegalArgumentException.class,
        () -> offerService.createOffer(testDemand.getId(), "   ", buildTestRestPartInfo()));
  }

  @Test
  void should_throw_exception_when_part_info_is_null() {
    assertThrows(
        IllegalArgumentException.class,
        () -> offerService.createOffer(testDemand.getId(), TEST_DESCRIPTION, null));
  }

  @Test
  void should_throw_exception_when_demand_not_found() {
    assertThrows(
        ResourceNotFoundException.class,
        () ->
            offerService.createOffer(
                "non-existent-demand", TEST_DESCRIPTION, buildTestRestPartInfo()));
  }

  @Test
  void should_throw_exception_when_demand_is_not_published() {
    val draftDemand = createTestDemand(createTestResearcher(), PostStatus.DRAFT);

    assertThrows(
        IllegalStateException.class,
        () ->
            offerService.createOffer(
                draftDemand.getId(), TEST_DESCRIPTION, buildTestRestPartInfo()));
  }

  @Test
  void should_throw_exception_when_seller_already_has_offer_for_demand() {
    offerService.createOffer(testDemand.getId(), TEST_DESCRIPTION, buildTestRestPartInfo());

    assertThrows(
        IllegalStateException.class,
        () ->
            offerService.createOffer(
                testDemand.getId(), TEST_DESCRIPTION, buildTestRestPartInfo()));
  }

  @Test
  void should_update_offer_status_to_published() {
    val offer = createPersistedOffer(testSeller, PostStatus.DRAFT);

    val updated = offerService.updateOfferStatus(offer.getId(), PostStatus.PUBLISHED);

    assertEquals(PostStatus.PUBLISHED, updated.getStatus());
    assertNull(updated.getCanceledAt());
    assertNull(updated.getSuspendedAt());
  }

  @Test
  void should_update_offer_status_to_canceled() {
    val offer = createPersistedOffer(testSeller, PostStatus.PUBLISHED);

    val updated = offerService.updateOfferStatus(offer.getId(), PostStatus.CANCELED);

    assertEquals(PostStatus.CANCELED, updated.getStatus());
    assertNotNull(updated.getCanceledAt());
  }

  @Test
  void should_update_offer_status_to_suspended() {
    val offer = createPersistedOffer(testSeller, PostStatus.PUBLISHED);

    val updated = offerService.updateOfferStatus(offer.getId(), PostStatus.SUSPENDED);

    assertEquals(PostStatus.SUSPENDED, updated.getStatus());
    assertNotNull(updated.getSuspendedAt());
  }

  @Test
  void should_update_suspended_offer_to_published() {
    val offer = createPersistedOffer(testSeller, PostStatus.SUSPENDED);

    val updated = offerService.updateOfferStatus(offer.getId(), PostStatus.PUBLISHED);

    assertEquals(PostStatus.PUBLISHED, updated.getStatus());
    assertNull(updated.getSuspendedAt());
    assertNull(updated.getCanceledAt());
  }

  @Test
  void should_throw_exception_when_updating_to_same_status() {
    val offer = createPersistedOffer(testSeller, PostStatus.PUBLISHED);

    assertThrows(
        IllegalStateException.class,
        () -> offerService.updateOfferStatus(offer.getId(), PostStatus.PUBLISHED));
  }

  @Test
  void should_throw_exception_when_updating_canceled_offer() {
    val offer = createPersistedOffer(testSeller, PostStatus.CANCELED);

    assertThrows(
        IllegalStateException.class,
        () -> offerService.updateOfferStatus(offer.getId(), PostStatus.PUBLISHED));
  }

  @Test
  void should_throw_exception_when_suspended_offer_transitions_to_non_published_status() {
    val offer = createPersistedOffer(testSeller, PostStatus.SUSPENDED);

    assertThrows(
        IllegalStateException.class,
        () -> offerService.updateOfferStatus(offer.getId(), PostStatus.DRAFT));
    assertThrows(
        IllegalStateException.class,
        () -> offerService.updateOfferStatus(offer.getId(), PostStatus.CANCELED));
  }

  @Test
  void should_throw_exception_when_offer_not_found() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> offerService.updateOfferStatus("non-existent-id", PostStatus.PUBLISHED));
  }

  @Test
  void should_throw_exception_when_offer_belongs_to_different_seller() {
    val otherSeller = createOtherSeller();
    val offer = createPersistedOffer(otherSeller, PostStatus.DRAFT);

    assertThrows(
        ResourceNotFoundException.class,
        () -> offerService.updateOfferStatus(offer.getId(), PostStatus.PUBLISHED));
  }

  @Test
  void should_get_seller_offers_by_status() {
    createPersistedOffer(testSeller, PostStatus.DRAFT);
    createPersistedOffer(testSeller, PostStatus.PUBLISHED);
    createPersistedOffer(testSeller, PostStatus.DRAFT);

    val drafts = offerService.getSellerOffersByStatus(PostStatus.DRAFT, DEFAULT_PAGE, DEFAULT_SIZE);

    assertEquals(2, drafts.getTotalElements());
    drafts
        .getContent()
        .forEach(
            o -> {
              assertEquals(PostStatus.DRAFT, o.getStatus());
              assertEquals(TEST_SELLER_ID, o.getSellerId());
            });
  }

  @Test
  void should_get_all_seller_offers() {
    createPersistedOffer(testSeller, PostStatus.DRAFT);
    createPersistedOffer(testSeller, PostStatus.PUBLISHED);
    createPersistedOffer(testSeller, PostStatus.CANCELED);

    val all = offerService.getAllSellerOffers(DEFAULT_PAGE, DEFAULT_SIZE);

    assertEquals(3, all.getTotalElements());
    all.getContent().forEach(o -> assertEquals(TEST_SELLER_ID, o.getSellerId()));
  }

  @Test
  void should_not_return_other_seller_offers() {
    val otherSeller = createOtherSeller();
    createPersistedOffer(testSeller, PostStatus.PUBLISHED);
    createPersistedOffer(otherSeller, PostStatus.PUBLISHED);

    val myOffers = offerService.getAllSellerOffers(DEFAULT_PAGE, DEFAULT_SIZE);

    assertEquals(1, myOffers.getTotalElements());
    assertEquals(TEST_SELLER_ID, myOffers.getContent().getFirst().getSellerId());
  }

  @Test
  void should_get_offer_by_id() {
    val created = createPersistedOffer(testSeller, PostStatus.PUBLISHED);

    val offer = offerService.getOfferById(created.getId());

    assertNotNull(offer);
    assertEquals(created.getId(), offer.getId());
    assertEquals(TEST_DESCRIPTION, offer.getDescription());
    assertEquals(TEST_SELLER_ID, offer.getSellerId());
    assertNotNull(offer.getDemand());
    assertEquals(testDemand.getId(), offer.getDemand().getId());
    assertNotNull(offer.getPartsInfo());
    assertNotNull(offer.getPartsInfo().getPart());
    assertEquals(TEST_PART_NAME, offer.getPartsInfo().getPart().getName());
  }

  @Test
  void should_throw_exception_when_getting_non_existent_offer() {
    assertThrows(
        ResourceNotFoundException.class, () -> offerService.getOfferById("non-existent-id"));
  }

  @Test
  void should_throw_exception_when_getting_other_seller_offer() {
    val otherSeller = createOtherSeller();
    val offer = createPersistedOffer(otherSeller, PostStatus.PUBLISHED);

    assertThrows(ResourceNotFoundException.class, () -> offerService.getOfferById(offer.getId()));
  }

  @Test
  void should_get_offers_by_demand_id() {
    val otherSeller = createOtherSeller();
    createPersistedOffer(testSeller, PostStatus.PUBLISHED);
    createPersistedOffer(otherSeller, PostStatus.PUBLISHED);

    val offers = offerService.getOffersByDemandId(testDemand.getId(), DEFAULT_PAGE, DEFAULT_SIZE);

    assertEquals(2, offers.getTotalElements());
  }

  @Test
  void should_return_empty_page_when_no_offers_match_status() {
    createPersistedOffer(testSeller, PostStatus.DRAFT);

    val published =
        offerService.getSellerOffersByStatus(PostStatus.PUBLISHED, DEFAULT_PAGE, DEFAULT_SIZE);

    assertEquals(0, published.getTotalElements());
    assertTrue(published.getContent().isEmpty());
  }

  @Test
  void should_handle_pagination_correctly() {
    for (int i = 0; i < 25; i++) createPersistedOffer(testSeller, PostStatus.PUBLISHED);

    val first =
        offerService.getSellerOffersByStatus(PostStatus.PUBLISHED, DEFAULT_PAGE, DEFAULT_SIZE);
    assertEquals(DEFAULT_SIZE, first.getContent().size());
    assertEquals(25, first.getTotalElements());
    assertEquals(3, first.getTotalPages());
    assertTrue(first.isFirst());
    assertFalse(first.isLast());

    val last = offerService.getSellerOffersByStatus(PostStatus.PUBLISHED, 2, DEFAULT_SIZE);
    assertEquals(5, last.getContent().size());
    assertTrue(last.isLast());
    assertFalse(last.isFirst());
  }

  private void authenticateSeller() {
    val auth =
        new UsernamePasswordAuthenticationToken(TEST_SELLER_SUPABASE_ID, null, new ArrayList<>());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private RestPartInfo buildTestRestPartInfo() {
    return new RestPartInfo(
        TEST_PART_NAME,
        TEST_CAR_BRAND,
        TEST_CAR_MODEL,
        Year.of(TEST_CAR_YEAR),
        PartCategory.FOG_LIGHTS,
        PartCondition.USED,
        BigDecimal.valueOf(TEST_PRICE),
        new ArrayList<>());
  }

  private JSeller createTestSeller() {
    return userRepository.save(
        JSeller.builder()
            .id(TEST_SELLER_ID)
            .supabaseUserId(TEST_SELLER_SUPABASE_ID)
            .name(TEST_SELLER_NAME)
            .email(TEST_EMAIL)
            .phoneNumber(TEST_PHONE)
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JSeller createOtherSeller() {
    return userRepository.save(
        JSeller.builder()
            .id("other-seller-" + currentTimeMillis())
            .supabaseUserId("other-supabase-" + currentTimeMillis())
            .name("Other Seller")
            .email("other-" + currentTimeMillis() + "@gmail.com")
            .phoneNumber("+9876543210")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JResearcher createTestResearcher() {
    return userRepository.save(
        JResearcher.builder()
            .id(TEST_RESEARCHER_ID + "-" + currentTimeMillis())
            .supabaseUserId(TEST_RESEARCHER_SUPABASE_ID + "-" + currentTimeMillis())
            .name("John Researcher")
            .email("researcher-" + currentTimeMillis() + "@gmail.com")
            .phoneNumber("+1111111111")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JDemand createTestDemand(JResearcher researcher, PostStatus status) {
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
            .description("Looking for " + TEST_PART_NAME)
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

  private JOffer createPersistedOffer(JSeller seller, PostStatus status) {
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
            .demand(testDemand)
            .description(TEST_DESCRIPTION)
            .attachedPhotoBucketKeys(new ArrayList<>())
            .partInfo(partInfo)
            .seller(seller)
            .status(status)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    if (status == PostStatus.CANCELED) offer.setCanceledAt(LocalDateTime.now());
    if (status == PostStatus.SUSPENDED) offer.setSuspendedAt(LocalDateTime.now());

    partInfo.setOffer(offer);
    return offerRepository.save(offer);
  }
}
