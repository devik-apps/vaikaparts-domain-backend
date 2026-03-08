package com.devikapps.vaikaparts.client;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devikapps.vaikaparts.client.api.OffersApi;
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.Offer;
import com.devikapps.vaikaparts.client.model.OfferPageResponse;
import com.devikapps.vaikaparts.conf.FacadeIT;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class OfferClientIT extends FacadeIT {

  private static final String BASE_URL = "http://localhost";
  private static final String OFFERS_ENDPOINT = "/v1/offers";

  private static final String TEST_SELLER_SUPABASE_ID = randomUUID().toString();
  private static final String TEST_RESEARCHER_SUPABASE_ID = randomUUID().toString();
  private static final String TEST_SELLER_ID = randomUUID().toString();
  private static final String TEST_RESEARCHER_ID = randomUUID().toString();

  private static final String TEST_DESCRIPTION = "Offering a headlight for Toyota Corolla 2015";
  private static final String TEST_PART_NAME = "Headlight";
  private static final String TEST_CAR_BRAND = "Toyota";
  private static final String TEST_CAR_MODEL = "Corolla";
  private static final int TEST_CAR_YEAR = 2015;
  private static final double TEST_PRICE = 150.00;

  @LocalServerPort private int port;

  @Autowired private OfferRepository offerRepository;
  @Autowired private DemandRepository demandRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ValueObjectMapper vom;
  @Autowired private TestRestTemplate restTemplate;

  private OffersApi authenticatedSellerClient;
  private OffersApi authenticatedResearcherClient;
  private OffersApi unauthenticatedClient;

  private JSeller testSeller;
  private JResearcher testResearcher;
  private JDemand testDemand;

  @BeforeEach
  void setUp() {
    testSeller = createTestSeller();
    testResearcher = createTestResearcher();
    testDemand = createPersistedDemand(testResearcher);

    authenticatedSellerClient = buildClient(JwtTestFactory.generateToken(TEST_SELLER_SUPABASE_ID));
    authenticatedResearcherClient =
        buildClient(JwtTestFactory.generateToken(TEST_RESEARCHER_SUPABASE_ID));
    unauthenticatedClient = buildClient(null);
  }

  @AfterEach
  void tearDown() {
    offerRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  // -------------------------------------------------------------------------
  // POST /v1/offers
  // -------------------------------------------------------------------------

  @Test
  void create_offer_test() {
    ResponseEntity<Offer> created =
        createOfferViaMultipart(testDemand.getId(), PartCategory.FOG_LIGHTS.name());

    assertThat(created.getStatusCode().value()).isEqualTo(HttpStatus.CREATED.value());

    Offer offer = created.getBody();
    assertThat(offer).isNotNull();
    assertThat(offer.getId()).isNotNull();
    assertThat(offer.getDescription()).isEqualTo(TEST_DESCRIPTION);
    assertThat(offer.getStatus()).isNotNull();
    assertThat(offer.getSellerId()).isNotNull();
    assertThat(offer.getDemand()).isNotNull();
    assertThat(offer.getDemand().getId().toString()).isEqualTo(testDemand.getId());
    assertThat(offer.getPartsInfo()).isNotNull();
    assertThat(offer.getPartsInfo().getPart()).isNotNull();
    assertThat(offer.getPartsInfo().getPart().getName()).isEqualTo(TEST_PART_NAME);
    assertThat(offer.getPartsInfo().getPrice()).isEqualTo(TEST_PRICE);
    assertThat(offer.getCreatedAt()).isNotNull();

    // 400 — blank description
    ResponseEntity<String> blankDescription =
        createOfferViaMultipartRaw(testDemand.getId(), "", PartCategory.FOG_LIGHTS.name());
    assertThat(blankDescription.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());

    // 404 — demand does not exist
    ResponseEntity<String> demandNotFound =
        createOfferViaMultipartRaw(
            "non-existent-demand-id", TEST_DESCRIPTION, PartCategory.FOG_LIGHTS.name());
    assertThat(demandNotFound.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());

    // 401 — unauthenticated request
    MultiValueMap<String, Object> formData =
        buildOfferFormData(testDemand.getId(), TEST_DESCRIPTION);
    ResponseEntity<String> unauthorized =
        restTemplate.exchange(
            format("%s:%s%s", BASE_URL, port, OFFERS_ENDPOINT),
            HttpMethod.POST,
            new HttpEntity<>(formData, new HttpHeaders()),
            String.class);
    assertThat(unauthorized.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

    // 400 — researcher role is not permitted to create offers
    ResponseEntity<String> forbidden = createOfferViaMultipartAs(testDemand.getId());
    assertThat(forbidden.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  // -------------------------------------------------------------------------
  // GET /v1/offers/{offerId}
  // -------------------------------------------------------------------------

  @Test
  void get_offer_by_id_test() {
    JOffer persisted = createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);

    ResponseEntity<Offer> ok =
        authenticatedSellerClient.getOfferByIdWithHttpInfo(persisted.getId());

    assertThat(ok.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    Offer offer = ok.getBody();
    assertThat(offer).isNotNull();
    assertThat(offer.getId().toString()).isEqualTo(persisted.getId());
    assertThat(offer.getDescription()).isEqualTo(TEST_DESCRIPTION);
    assertThat(offer.getStatus()).isNotNull();
    assertThat(offer.getSellerId()).isNotNull();
    assertThat(offer.getDemand()).isNotNull();
    assertThat(offer.getPartsInfo()).isNotNull();
    assertThat(offer.getPartsInfo().getPrice()).isEqualTo(TEST_PRICE);

    // 404 — offer does not exist
    assertThatThrownBy(() -> authenticatedSellerClient.getOfferById("non-existent-id"))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.NOT_FOUND.value()));

    // 404 — offer belongs to another seller (information disclosure prevention)
    JSeller otherSeller = createOtherSeller();
    JOffer otherOffer = createPersistedOffer(otherSeller, testDemand, PostStatus.PUBLISHED);

    assertThatThrownBy(() -> authenticatedSellerClient.getOfferById(otherOffer.getId()))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.NOT_FOUND.value()));

    // 401 — unauthenticated
    assertThatThrownBy(() -> unauthenticatedClient.getOfferById(persisted.getId()))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
  }

  // -------------------------------------------------------------------------
  // GET /v1/offers/demand/{demandId}
  // -------------------------------------------------------------------------

  @Test
  void get_offers_by_demand_id_test() {
    JSeller otherSeller = createOtherSeller();
    JDemand secondDemand = createPersistedDemand(testResearcher);

    createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);
    createPersistedOffer(otherSeller, testDemand, PostStatus.PUBLISHED);
    createPersistedOffer(testSeller, secondDemand, PostStatus.PUBLISHED);

    ResponseEntity<OfferPageResponse> ok =
        authenticatedSellerClient.getOffersByDemandIdWithHttpInfo(testDemand.getId(), 0, 10);

    assertThat(ok.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    OfferPageResponse page = ok.getBody();
    assertThat(page).isNotNull();
    assertThat(page.getContent()).hasSize(2);
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getPageable()).isNotNull();
    page.getContent()
        .forEach(
            o -> {
              assertThat(o.getDemand()).isNotNull();
              assertThat(o.getDemand().getId().toString()).isEqualTo(testDemand.getId());
            });

    JDemand emptyDemand = createPersistedDemand(testResearcher);

    ResponseEntity<OfferPageResponse> empty =
        authenticatedSellerClient.getOffersByDemandIdWithHttpInfo(emptyDemand.getId(), 0, 10);

    assertThat(empty.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
    assertThat(empty.getBody()).isNotNull();
    assertThat(empty.getBody().getContent()).isEmpty();
    assertThat(empty.getBody().getTotalElements()).isZero();

    // 401 — unauthenticated
    assertThatThrownBy(() -> unauthenticatedClient.getOffersByDemandId(testDemand.getId(), 0, 10))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
  }

  // -------------------------------------------------------------------------
  // GET /v1/demands/{demandId}/offers
  // -------------------------------------------------------------------------

  @Test
  void get_offers_for_demand_test() {
    JSeller otherSeller = createOtherSeller();
    createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);
    createPersistedOffer(otherSeller, testDemand, PostStatus.PUBLISHED);

    ResponseEntity<OfferPageResponse> ok =
        authenticatedResearcherClient.getOffersForDemandWithHttpInfo(testDemand.getId(), 0, 10);

    assertThat(ok.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    OfferPageResponse page = ok.getBody();
    assertThat(page).isNotNull();
    assertThat(page.getContent()).hasSize(2);
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getPageable()).isNotNull();
    page.getContent()
        .forEach(
            o -> {
              assertThat(o.getId()).isNotNull();
              assertThat(o.getStatus()).isNotNull();
              assertThat(o.getDemand()).isNotNull();
              assertThat(o.getPartsInfo()).isNotNull();
            });

    // Empty page — demand has no offers yet
    JDemand emptyDemand = createPersistedDemand(testResearcher);

    ResponseEntity<OfferPageResponse> empty =
        authenticatedResearcherClient.getOffersForDemandWithHttpInfo(emptyDemand.getId(), 0, 10);

    assertThat(empty.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
    assertThat(empty.getBody()).isNotNull();
    assertThat(empty.getBody().getContent()).isEmpty();

    // 401 — unauthenticated
    assertThatThrownBy(() -> unauthenticatedClient.getOffersForDemand(testDemand.getId(), 0, 10))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
  }

  // -------------------------------------------------------------------------
  // GET /v1/offers
  // -------------------------------------------------------------------------

  @Test
  void get_seller_offers_test() {
    JDemand secondDemand = createPersistedDemand(testResearcher);

    createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);
    createPersistedOffer(testSeller, secondDemand, PostStatus.DRAFT);

    ResponseEntity<OfferPageResponse> all =
        authenticatedSellerClient.getSellerOffersWithHttpInfo(0, 10, null);

    assertThat(all.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    OfferPageResponse allPage = all.getBody();
    assertThat(allPage).isNotNull();
    assertThat(allPage.getContent()).hasSize(2);
    assertThat(allPage.getTotalElements()).isEqualTo(2);
    assertThat(allPage.getPageable()).isNotNull();
    assertThat(allPage.getTotalPages()).isNotNull();
    allPage
        .getContent()
        .forEach(
            o -> {
              assertThat(o.getId()).isNotNull();
              assertThat(o.getStatus()).isNotNull();
              assertThat(o.getSellerId()).isNotNull();
              assertThat(o.getDemand()).isNotNull();
              assertThat(o.getPartsInfo()).isNotNull();
            });

    JDemand thirdDemand = createPersistedDemand(testResearcher);
    createPersistedOffer(testSeller, thirdDemand, PostStatus.DRAFT);

    ResponseEntity<OfferPageResponse> filtered =
        authenticatedSellerClient.getSellerOffersWithHttpInfo(
            0, 10, com.devikapps.vaikaparts.client.model.PostStatus.PENDING);

    assertThat(filtered.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    OfferPageResponse filteredPage = filtered.getBody();
    assertThat(filteredPage).isNotNull();
    assertThat(filteredPage.getContent()).hasSize(0);
    filteredPage
        .getContent()
        .forEach(
            o ->
                assertThat(o.getStatus())
                    .isEqualTo(com.devikapps.vaikaparts.client.model.PostStatus.PENDING));

    // Isolation — offers from another seller are never returned
    JSeller otherSeller = createOtherSeller();
    JDemand fourthDemand = createPersistedDemand(testResearcher);
    createPersistedOffer(otherSeller, fourthDemand, PostStatus.PUBLISHED);

    ResponseEntity<OfferPageResponse> isolated =
        authenticatedSellerClient.getSellerOffersWithHttpInfo(0, 10, null);

    assertThat(isolated.getBody()).isNotNull();
    isolated
        .getBody()
        .getContent()
        .forEach(o -> assertThat(o.getSellerId().toString()).isEqualTo(TEST_SELLER_ID));

    // 401 — unauthenticated
    assertThatThrownBy(() -> unauthenticatedClient.getSellerOffers(0, 10, null))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
  }

  // -------------------------------------------------------------------------
  // PATCH /v1/offers/{offerId}/status
  // -------------------------------------------------------------------------

  @Test
  void update_offer_status_test() {
    JOffer persisted = createPersistedOffer(testSeller, testDemand, PostStatus.DRAFT);

    ResponseEntity<Offer> ok =
        authenticatedSellerClient.updateOfferStatusWithHttpInfo(
            persisted.getId(), com.devikapps.vaikaparts.client.model.PostStatus.PUBLISHED);

    assertThat(ok.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    Offer updated = ok.getBody();
    assertThat(updated).isNotNull();
    assertThat(updated.getId().toString()).isEqualTo(persisted.getId());
    assertThat(updated.getStatus())
        .isEqualTo(com.devikapps.vaikaparts.client.model.PostStatus.PUBLISHED);

    // 400 — invalid transition (same status)
    assertThatThrownBy(
            () ->
                authenticatedSellerClient.updateOfferStatus(
                    persisted.getId(), com.devikapps.vaikaparts.client.model.PostStatus.PUBLISHED))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.BAD_REQUEST.value()));

    // 404 — offer does not exist
    assertThatThrownBy(
            () ->
                authenticatedSellerClient.updateOfferStatus(
                    "non-existent-id", com.devikapps.vaikaparts.client.model.PostStatus.PUBLISHED))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.NOT_FOUND.value()));

    // 404 — offer belongs to another seller
    JSeller otherSeller = createOtherSeller();
    JOffer otherOffer = createPersistedOffer(otherSeller, testDemand, PostStatus.DRAFT);

    assertThatThrownBy(
            () ->
                authenticatedSellerClient.updateOfferStatus(
                    otherOffer.getId(), com.devikapps.vaikaparts.client.model.PostStatus.PUBLISHED))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.NOT_FOUND.value()));

    // 401 — unauthenticated
    JOffer another =
        createPersistedOffer(testSeller, createPersistedDemand(testResearcher), PostStatus.DRAFT);

    assertThatThrownBy(
            () ->
                unauthenticatedClient.updateOfferStatus(
                    another.getId(), com.devikapps.vaikaparts.client.model.PostStatus.PUBLISHED))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
  }

  private OffersApi buildClient(String bearerToken) {
    RestClient.Builder builder = RestClient.builder().baseUrl(format("%s:%s", BASE_URL, port));

    if (bearerToken != null) builder.defaultHeader("Authorization", "Bearer " + bearerToken);

    ApiClient apiClient = new ApiClient(builder.build());
    apiClient.setBasePath(format("%s:%s", BASE_URL, port));
    return new OffersApi(apiClient);
  }

  private MultiValueMap<String, Object> buildOfferFormData(String demandId, String description) {
    MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
    formData.add("demandId", demandId);
    formData.add("description", description);
    formData.add("partInfo.name", TEST_PART_NAME);
    formData.add("partInfo.carBrand", TEST_CAR_BRAND);
    formData.add("partInfo.carModel", TEST_CAR_MODEL);
    formData.add("partInfo.carYear", String.valueOf(TEST_CAR_YEAR));
    formData.add("partInfo.partCategory", PartCategory.FOG_LIGHTS.name());
    formData.add("partInfo.condition", PartCondition.USED.name());
    formData.add("partInfo.price", String.valueOf(TEST_PRICE));
    return formData;
  }

  private ResponseEntity<Offer> createOfferViaMultipart(String demandId, String partCategory) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setBearerAuth(JwtTestFactory.generateToken(TEST_SELLER_SUPABASE_ID));

    MultiValueMap<String, Object> formData =
        buildOfferFormData(demandId, OfferClientIT.TEST_DESCRIPTION);
    formData.set("partInfo.partCategory", partCategory);

    return restTemplate.exchange(
        format("%s:%s%s", BASE_URL, port, OFFERS_ENDPOINT),
        HttpMethod.POST,
        new HttpEntity<>(formData, headers),
        Offer.class);
  }

  private ResponseEntity<String> createOfferViaMultipartRaw(
      String demandId, String description, String partCategory) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setBearerAuth(JwtTestFactory.generateToken(TEST_SELLER_SUPABASE_ID));

    MultiValueMap<String, Object> formData = buildOfferFormData(demandId, description);
    formData.set("partInfo.partCategory", partCategory);

    return restTemplate.exchange(
        format("%s:%s%s", BASE_URL, port, OFFERS_ENDPOINT),
        HttpMethod.POST,
        new HttpEntity<>(formData, headers),
        String.class);
  }

  private ResponseEntity<String> createOfferViaMultipartAs(String demandId) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setBearerAuth(JwtTestFactory.generateToken(OfferClientIT.TEST_RESEARCHER_SUPABASE_ID));

    return restTemplate.exchange(
        format("%s:%s%s", BASE_URL, port, OFFERS_ENDPOINT),
        HttpMethod.POST,
        new HttpEntity<>(buildOfferFormData(demandId, OfferClientIT.TEST_DESCRIPTION), headers),
        String.class);
  }

  private JSeller createTestSeller() {
    return userRepository.save(
        JSeller.builder()
            .id(OfferClientIT.TEST_SELLER_ID)
            .supabaseUserId(OfferClientIT.TEST_SELLER_SUPABASE_ID)
            .name("Contract Test Seller")
            .email("seller@test.com")
            .phoneNumber("+1234567890")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JSeller createOtherSeller() {
    return userRepository.save(
        JSeller.builder()
            .id(randomUUID().toString())
            .supabaseUserId(randomUUID().toString())
            .name("Other Seller")
            .email(randomUUID() + "@test.com")
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
            .id(OfferClientIT.TEST_RESEARCHER_ID)
            .supabaseUserId(OfferClientIT.TEST_RESEARCHER_SUPABASE_ID)
            .name("Contract Test Researcher")
            .phoneNumber("+1111111111")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JDemand createPersistedDemand(JResearcher researcher) {
    val part =
        JPart.builder()
            .id(randomUUID().toString())
            .partName(TEST_PART_NAME)
            .carBrand(TEST_CAR_BRAND)
            .carModel(TEST_CAR_MODEL)
            .carYear(TEST_CAR_YEAR)
            .partCategory(PartCategory.FOG_LIGHTS)
            .imageBuckets(new ArrayList<>())
            .build();

    val demand =
        JDemand.builder()
            .id(randomUUID().toString())
            .description("Looking for " + TEST_PART_NAME)
            .attachedPhotoBucketKeys(new ArrayList<>())
            .part(part)
            .researcher(researcher)
            .status(com.devikapps.vaikaparts.model.classifier.PostStatus.PUBLISHED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    part.setDemand(demand);
    return demandRepository.save(demand);
  }

  private JOffer createPersistedOffer(
      JSeller seller, JDemand demand, com.devikapps.vaikaparts.model.classifier.PostStatus status) {
    val partInfo =
        JPartInfo.builder()
            .id(randomUUID().toString())
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
            .id(randomUUID().toString())
            .demand(demand)
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
