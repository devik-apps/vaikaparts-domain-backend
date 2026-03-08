package com.devikapps.vaikaparts.client;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devikapps.vaikaparts.client.api.DemandsApi;
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.Demand;
import com.devikapps.vaikaparts.client.model.DemandPageResponse;
import com.devikapps.vaikaparts.client.model.PartCategory;
import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

class DemandClientIT extends FacadeIT {

  private static final String TEST_SUPABASE_USER_ID = randomUUID().toString();
  private static final String TEST_RESEARCHER_ID = randomUUID().toString();
  private static final String TEST_DESCRIPTION = "Looking for headlight";
  private static final String TEST_PART_NAME = "Headlight";
  private static final String TEST_CAR_BRAND = "Toyota";
  private static final String TEST_CAR_MODEL = "Corolla";
  private static final int TEST_CAR_YEAR = 2015;
  private static final String BASE_URL = "http://localhost";

  @LocalServerPort private int port;

  @Autowired private DemandRepository demandRepository;
  @Autowired private OfferRepository offerRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ValueObjectMapper vom;
  @Autowired private TestRestTemplate restTemplate;

  private DemandsApi authenticatedClient;
  private DemandsApi unauthenticatedClient;

  @BeforeEach
  void setUp() {
    createTestResearcher();
    String token = JwtTestFactory.generateToken(TEST_SUPABASE_USER_ID);
    authenticatedClient = buildClient(token);
    unauthenticatedClient = buildClient(null);
  }

  @AfterEach
  void tearDown() {
    offerRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  // -------------------------------------------------------------------------
  // POST /v1/demands
  // -------------------------------------------------------------------------

  @Test
  void create_demand_test() {
    ResponseEntity<Demand> created = createDemandViaMultipart(PartCategory.FOG_LIGHTS.name());

    assertThat(created.getStatusCode().value()).isEqualTo(HttpStatus.CREATED.value());

    Demand demand = created.getBody();
    assertThat(demand).isNotNull();
    assertThat(demand.getId().toString()).isNotBlank();
    assertThat(demand.getDescription()).isEqualTo(TEST_DESCRIPTION);
    assertThat(demand.getStatus()).isNotNull();
    assertThat(demand.getResearcher()).isNotNull();
    assertThat(demand.getResearcher().getId().toString()).isEqualTo(TEST_RESEARCHER_ID);
    assertThat(demand.getPart()).isNotNull();
    assertThat(demand.getPart().getName()).isEqualTo(TEST_PART_NAME);
    assertThat(demand.getPart().getCarBrand()).isEqualTo(TEST_CAR_BRAND);

    // 400 — blank description
    ResponseEntity<String> badRequest = createDemandViaMultipartRaw();
    assertThat(badRequest.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());

    // 401 — unauthenticated
    MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
    formData.add("description", TEST_DESCRIPTION);
    formData.add("part.name", TEST_PART_NAME);
    formData.add("part.carBrand", TEST_CAR_BRAND);
    formData.add("part.carModel", TEST_CAR_MODEL);
    formData.add("part.carYear", String.valueOf(TEST_CAR_YEAR));
    formData.add("part.partCategory", PartCategory.FOG_LIGHTS.name());

    ResponseEntity<String> unauthorized =
        restTemplate.exchange(
            format("%s:%s/v1/demands", BASE_URL, port),
            HttpMethod.POST,
            new HttpEntity<>(formData, new HttpHeaders()),
            String.class);

    assertThat(unauthorized.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
  }

  // -------------------------------------------------------------------------
  // GET /v1/demands
  // -------------------------------------------------------------------------

  @Test
  void get_researcher_demands_test() {
    createPersistedDemand(PostStatus.DRAFT);
    createPersistedDemand(PostStatus.PUBLISHED);

    ResponseEntity<DemandPageResponse> ok =
        authenticatedClient.getResearcherDemandsWithHttpInfo(0, 10, null);

    assertThat(ok.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    DemandPageResponse page = ok.getBody();
    assertThat(page).isNotNull();
    assertThat(page.getContent()).hasSize(2);
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getTotalPages()).isNotNull();
    assertThat(page.getPageable()).isNotNull();

    Demand first = page.getContent().getFirst();
    assertThat(first.getId().toString()).isNotBlank();
    assertThat(first.getDescription()).isNotBlank();
    assertThat(first.getStatus()).isNotNull();
    assertThat(first.getResearcher()).isNotNull();
    assertThat(first.getPart()).isNotNull();

    // Status filter — only DRAFT demands returned
    createPersistedDemand(PostStatus.DRAFT);

    ResponseEntity<DemandPageResponse> filtered =
        authenticatedClient.getResearcherDemandsWithHttpInfo(
            0, 10, com.devikapps.vaikaparts.client.model.PostStatus.DRAFT);

    assertThat(filtered.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    DemandPageResponse filteredPage = filtered.getBody();
    assertThat(filteredPage).isNotNull();
    assertThat(filteredPage.getContent()).hasSize(2);
    filteredPage
        .getContent()
        .forEach(
            d ->
                assertThat(d.getStatus())
                    .isEqualTo(com.devikapps.vaikaparts.client.model.PostStatus.DRAFT));

    // 401 — unauthenticated
    assertThatThrownBy(() -> unauthenticatedClient.getResearcherDemands(0, 10, null))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
  }

  // -------------------------------------------------------------------------
  // GET /v1/demands/{demandId}
  // -------------------------------------------------------------------------

  @Test
  void get_demand_by_id_test() {
    // Happy path — demand retrieved successfully
    JDemand persisted = createPersistedDemand(PostStatus.PUBLISHED);

    ResponseEntity<Demand> ok = authenticatedClient.getDemandByIdWithHttpInfo(persisted.getId());

    assertThat(ok.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    Demand demand = ok.getBody();
    assertThat(demand).isNotNull();
    assertThat(demand.getId().toString()).isEqualTo(persisted.getId());
    assertThat(demand.getDescription()).isEqualTo(TEST_DESCRIPTION);
    assertThat(demand.getStatus()).isNotNull();
    assertThat(demand.getResearcher()).isNotNull();
    assertThat(demand.getPart()).isNotNull();

    // 404 — nonexistent demand
    assertThatThrownBy(() -> authenticatedClient.getDemandById("nonexistent-id"))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.NOT_FOUND.value()));

    // 401 — unauthenticated
    assertThatThrownBy(() -> unauthenticatedClient.getDemandById(persisted.getId()))
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
    JDemand persisted = createPersistedDemand(PostStatus.PUBLISHED);

    ResponseEntity<?> ok =
        authenticatedClient.getOffersForDemandWithHttpInfo(persisted.getId(), 0, 10);

    assertThat(ok.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    // 401 — unauthenticated
    assertThatThrownBy(() -> unauthenticatedClient.getOffersForDemand(persisted.getId(), 0, 10))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
  }

  // -------------------------------------------------------------------------
  // PATCH /v1/demands/{demandId}/status
  // -------------------------------------------------------------------------

  @Test
  void update_demand_status_test() {
    // Happy path — status transitioned successfully
    JDemand persisted = createPersistedDemand(PostStatus.DRAFT);

    ResponseEntity<Demand> ok =
        authenticatedClient.updateDemandStatusWithHttpInfo(
            persisted.getId(), com.devikapps.vaikaparts.client.model.PostStatus.PUBLISHED);

    assertThat(ok.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    Demand demand = ok.getBody();
    assertThat(demand).isNotNull();
    assertThat(demand.getId().toString()).isEqualTo(persisted.getId());
    assertThat(demand.getStatus())
        .isEqualTo(com.devikapps.vaikaparts.client.model.PostStatus.PUBLISHED);

    // 404 — nonexistent demand
    assertThatThrownBy(
            () ->
                authenticatedClient.updateDemandStatus(
                    "nonexistent-id", com.devikapps.vaikaparts.client.model.PostStatus.PUBLISHED))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.NOT_FOUND.value()));

    // 401 — unauthenticated
    JDemand another = createPersistedDemand(PostStatus.DRAFT);

    assertThatThrownBy(
            () ->
                unauthenticatedClient.updateDemandStatus(
                    another.getId(), com.devikapps.vaikaparts.client.model.PostStatus.PUBLISHED))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
  }

  private DemandsApi buildClient(String bearerToken) {
    RestClient.Builder builder = RestClient.builder().baseUrl(format("%s:%s", BASE_URL, port));

    if (bearerToken != null) builder.defaultHeader("Authorization", "Bearer " + bearerToken);

    ApiClient apiClient = new ApiClient(builder.build());
    apiClient.setBasePath(format("%s:%s", BASE_URL, port));
    return new DemandsApi(apiClient);
  }

  private ResponseEntity<Demand> createDemandViaMultipart(String partCategory) {
    MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
    formData.add("description", TEST_DESCRIPTION);
    formData.add("part.name", TEST_PART_NAME);
    formData.add("part.carBrand", TEST_CAR_BRAND);
    formData.add("part.carModel", TEST_CAR_MODEL);
    formData.add("part.carYear", String.valueOf(TEST_CAR_YEAR));
    formData.add("part.partCategory", partCategory);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setBearerAuth(JwtTestFactory.generateToken(TEST_SUPABASE_USER_ID));

    return restTemplate.exchange(
        format("%s:%s/v1/demands", BASE_URL, port),
        HttpMethod.POST,
        new HttpEntity<>(formData, headers),
        Demand.class);
  }

  private ResponseEntity<String> createDemandViaMultipartRaw() {
    MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
    formData.add("description", "");
    formData.add("part.name", TEST_PART_NAME);
    formData.add("part.carBrand", TEST_CAR_BRAND);
    formData.add("part.carModel", TEST_CAR_MODEL);
    formData.add("part.carYear", String.valueOf(TEST_CAR_YEAR));
    formData.add("part.partCategory", PartCategory.FOG_LIGHTS.name());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setBearerAuth(JwtTestFactory.generateToken(TEST_SUPABASE_USER_ID));

    return restTemplate.exchange(
        format("%s:%s/v1/demands", BASE_URL, port),
        HttpMethod.POST,
        new HttpEntity<>(formData, headers),
        String.class);
  }

  private void createTestResearcher() {
    userRepository.save(
        JResearcher.builder()
            .id(TEST_RESEARCHER_ID)
            .supabaseUserId(TEST_SUPABASE_USER_ID)
            .name("Contract Test Researcher")
            .phoneNumber("+1234567890")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JDemand createPersistedDemand(PostStatus status) {
    val part =
        JPart.builder()
            .id(randomUUID().toString())
            .partName(TEST_PART_NAME)
            .carBrand(TEST_CAR_BRAND)
            .carModel(TEST_CAR_MODEL)
            .carYear(TEST_CAR_YEAR)
            .partCategory(com.devikapps.vaikaparts.model.classifier.PartCategory.FOG_LIGHTS)
            .imageBuckets(List.of())
            .build();

    val demand =
        JDemand.builder()
            .id(randomUUID().toString())
            .description(TEST_DESCRIPTION)
            .attachedPhotoBucketKeys(new ArrayList<>())
            .part(part)
            .researcher((JResearcher) testResearcher())
            .status(status)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    part.setDemand(demand);
    return demandRepository.save(demand);
  }

  private JUser testResearcher() {
    return userRepository.findById(TEST_RESEARCHER_ID).orElseThrow();
  }
}
