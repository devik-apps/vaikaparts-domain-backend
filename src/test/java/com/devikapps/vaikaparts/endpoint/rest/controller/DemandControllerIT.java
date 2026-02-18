package com.devikapps.vaikaparts.endpoint.rest.controller;

import static java.lang.System.currentTimeMillis;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.util.Collections;
import java.util.List;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

class DemandControllerIT extends FacadeIT {

  private static final String DEMANDS_ENDPOINT = "/v1/demands";
  private static final String TEST_RESEARCHER_ID = "researcher-123";
  private static final String TEST_RESEARCHER_NAME = "John Doe";
  private static final String TEST_PHONE = "+1234567890";
  private static final String TEST_EMAIL = "test@gmail.com";
  private static final String TEST_SUPABASE_USER_ID = "supabase-user-123";
  private static final String TEST_DESCRIPTION = "Looking for headlight";
  private static final String TEST_PART_NAME = "Headlight";
  private static final String TEST_CAR_BRAND = "Toyota";
  private static final String TEST_CAR_MODEL = "Corolla";
  private static final int TEST_CAR_YEAR = 2015;
  @Autowired OfferRepository offerRepository;
  @Autowired private MockMvc mockMvc;
  @Autowired private DemandRepository demandRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ValueObjectMapper vom;

  private JResearcher testResearcher;

  @BeforeEach
  void setUp() {
    testResearcher = createTestResearcher();
    createTestSeller();
  }

  @AfterEach
  void tearDown() {
    offerRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void should_create_demand_successfully() throws Exception {
    authenticateUser();
    mockMvc
        .perform(
            multipart(DEMANDS_ENDPOINT)
                .param("description", TEST_DESCRIPTION)
                .param("part.name", TEST_PART_NAME)
                .param("part.carBrand", TEST_CAR_BRAND)
                .param("part.carModel", TEST_CAR_MODEL)
                .param("part.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("part.partCategory", PartCategory.FOG_LIGHTS.name())
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
        .andExpect(jsonPath("$.status").value(PostStatus.DRAFT.name()))
        .andExpect(jsonPath("$.researcher.id").value(TEST_RESEARCHER_ID))
        .andExpect(jsonPath("$.part").isNotEmpty())
        .andExpect(jsonPath("$.part.name").value(TEST_PART_NAME))
        .andExpect(jsonPath("$.part.car_brand").value(TEST_CAR_BRAND));
  }

  @Test
  void should_create_demand_with_single_image() throws Exception {
    authenticateUser();

    val imageFile =
        new MockMultipartFile(
            "part.images", "headlight.jpg", "image/jpeg", "fake image content".getBytes());

    mockMvc
        .perform(
            multipart(DEMANDS_ENDPOINT)
                .file(imageFile)
                .param("description", TEST_DESCRIPTION)
                .param("part.name", TEST_PART_NAME)
                .param("part.carBrand", TEST_CAR_BRAND)
                .param("part.carModel", TEST_CAR_MODEL)
                .param("part.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("part.partCategory", PartCategory.FOG_LIGHTS.name())
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
        .andExpect(jsonPath("$.part.image_urls").isArray())
        .andExpect(jsonPath("$.part.image_urls", hasSize(1)));
  }

  @Test
  void should_create_demand_with_multiple_images() throws Exception {
    authenticateUser();

    val imageFile1 =
        new MockMultipartFile(
            "part.images", "headlight-front.jpg", "image/jpeg", "fake image content 1".getBytes());

    val imageFile2 =
        new MockMultipartFile(
            "part.images", "headlight-side.jpg", "image/jpeg", "fake image content 2".getBytes());

    val imageFile3 =
        new MockMultipartFile(
            "part.images", "headlight-back.jpg", "image/jpeg", "fake image content 3".getBytes());

    mockMvc
        .perform(
            multipart(DEMANDS_ENDPOINT)
                .file(imageFile1)
                .file(imageFile2)
                .file(imageFile3)
                .param("description", TEST_DESCRIPTION)
                .param("part.name", TEST_PART_NAME)
                .param("part.carBrand", TEST_CAR_BRAND)
                .param("part.carModel", TEST_CAR_MODEL)
                .param("part.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("part.partCategory", PartCategory.FOG_LIGHTS.name())
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
        .andExpect(jsonPath("$.part.image_urls").isArray())
        .andExpect(jsonPath("$.part.image_urls", hasSize(3)));
  }

  @Test
  void should_return_400_when_description_is_blank() throws Exception {
    authenticateUser();
    mockMvc
        .perform(
            multipart(DEMANDS_ENDPOINT)
                .param("description", "")
                .param("part.name", TEST_PART_NAME)
                .param("part.carBrand", TEST_CAR_BRAND)
                .param("part.carModel", TEST_CAR_MODEL)
                .param("part.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("part.partCategory", PartCategory.FOG_LIGHTS.name())
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_401_when_not_authenticated() throws Exception {
    mockMvc
        .perform(
            multipart(DEMANDS_ENDPOINT)
                .param("description", TEST_DESCRIPTION)
                .param("part.name", TEST_PART_NAME)
                .param("part.carBrand", TEST_CAR_BRAND)
                .param("part.carModel", TEST_CAR_MODEL)
                .param("part.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("part.partCategory", PartCategory.FOG_LIGHTS.name())
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void should_get_researcher_demands_by_status() throws Exception {
    authenticateUser();
    createTestDemand(PostStatus.DRAFT);
    createTestDemand(PostStatus.PUBLISHED);
    createTestDemand(PostStatus.DRAFT);

    mockMvc
        .perform(
            get(DEMANDS_ENDPOINT)
                .param("status", PostStatus.DRAFT.name())
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].status").value(PostStatus.DRAFT.name()))
        .andExpect(jsonPath("$.content[1].status").value(PostStatus.DRAFT.name()))
        .andExpect(jsonPath("$.number_of_elements").value(2))
        .andExpect(jsonPath("$.pageable.page_number").value(0));
  }

  @Test
  void should_get_all_researcher_demands() throws Exception {
    authenticateUser();
    createTestDemand(PostStatus.DRAFT);
    createTestDemand(PostStatus.PUBLISHED);
    createTestDemand(PostStatus.CANCELED);

    mockMvc
        .perform(get(DEMANDS_ENDPOINT).param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.total_elements").value(3));
  }

  @Test
  void should_get_all_researcher_demands_without_pagination_params() throws Exception {
    authenticateUser();
    createTestDemand(PostStatus.DRAFT);
    createTestDemand(PostStatus.PUBLISHED);

    mockMvc
        .perform(get(DEMANDS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  void should_get_demand_by_id() throws Exception {
    authenticateUser();
    val demand = createTestDemand(PostStatus.PUBLISHED);

    mockMvc
        .perform(get(DEMANDS_ENDPOINT + "/{demandId}", demand.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(demand.getId()))
        .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
        .andExpect(jsonPath("$.researcher.id").value(TEST_RESEARCHER_ID));
  }

  @Test
  void should_return_404_when_getting_non_existent_demand() throws Exception {
    authenticateUser();
    mockMvc
        .perform(get(DEMANDS_ENDPOINT + "/{demandId}", "non-existent-id"))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_return_404_when_getting_other_researcher_demand() throws Exception {
    authenticateUser();
    val otherResearcher = createOtherResearcher();
    val otherDemand = createDemandForResearcher(otherResearcher, PostStatus.PUBLISHED);

    mockMvc
        .perform(get(DEMANDS_ENDPOINT + "/{demandId}", otherDemand.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_return_401_when_getting_demands_without_authentication() throws Exception {
    mockMvc.perform(get(DEMANDS_ENDPOINT)).andExpect(status().isUnauthorized());
  }

  @Test
  void should_handle_pagination_correctly() throws Exception {
    authenticateUser();
    for (int i = 0; i < 25; i++) {
      createTestDemand(PostStatus.PUBLISHED);
    }

    mockMvc
        .perform(
            get(DEMANDS_ENDPOINT)
                .param("status", PostStatus.PUBLISHED.name())
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(10)))
        .andExpect(jsonPath("$.total_elements").value(25))
        .andExpect(jsonPath("$.total_pages").value(3))
        .andExpect(jsonPath("$.first").value(true))
        .andExpect(jsonPath("$.last").value(false));

    mockMvc
        .perform(
            get(DEMANDS_ENDPOINT)
                .param("status", PostStatus.PUBLISHED.name())
                .param("page", "2")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(5)))
        .andExpect(jsonPath("$.first").value(false))
        .andExpect(jsonPath("$.last").value(true));
  }

  @Test
  void should_update_demand_status_to_published() throws Exception {
    authenticateUser();
    var demand = createTestDemand(PostStatus.DRAFT);

    mockMvc
        .perform(
            patch(DEMANDS_ENDPOINT + "/{demandId}/status", demand.getId())
                .param("status", PostStatus.PUBLISHED.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(demand.getId()))
        .andExpect(jsonPath("$.status").value(PostStatus.PUBLISHED.name()))
        .andExpect(jsonPath("$.canceled_at").doesNotExist())
        .andExpect(jsonPath("$.suspended_at").doesNotExist());
  }

  @Test
  void should_not_return_other_researcher_demands() throws Exception {
    authenticateUser();

    val otherResearcher = createOtherResearcher();
    createTestDemand(PostStatus.PUBLISHED);
    createDemandForResearcher(otherResearcher, PostStatus.PUBLISHED);

    mockMvc
        .perform(get(DEMANDS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].researcher.id").value(TEST_RESEARCHER_ID));
  }

  private void createTestSeller() {
    userRepository.save(
        JSeller.builder()
            .id("seller-123")
            .supabaseUserId("supabase-seller-123")
            .name("Jane Seller")
            .email("seller@gmail.com")
            .phoneNumber("+9876543210")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .build());
  }

  @Test
  void should_get_offers_for_demand() throws Exception {
    authenticateUser();
    val demand = createTestDemand(PostStatus.PUBLISHED);
    val seller1 = createTestSeller("seller-1");
    val seller2 = createTestSeller("seller-2");

    createPersistedOffer(seller1, demand);
    createPersistedOffer(seller2, demand);

    mockMvc
        .perform(
            get(DEMANDS_ENDPOINT + "/{demandId}/offers", demand.getId())
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].demand.id").value(demand.getId()))
        .andExpect(jsonPath("$.content[1].demand.id").value(demand.getId()))
        .andExpect(jsonPath("$.total_elements").value(2));
  }

  @Test
  void should_return_empty_page_when_no_offers_for_demand() throws Exception {
    authenticateUser();
    val demand = createTestDemand(PostStatus.PUBLISHED);

    mockMvc
        .perform(get(DEMANDS_ENDPOINT + "/{demandId}/offers", demand.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(0)))
        .andExpect(jsonPath("$.total_elements").value(0));
  }

  @Test
  void should_return_404_when_getting_offers_for_other_researcher_demand() throws Exception {
    authenticateUser();
    val otherResearcher = createOtherResearcher();
    val otherDemand = createDemandForResearcher(otherResearcher, PostStatus.PUBLISHED);

    mockMvc
        .perform(get(DEMANDS_ENDPOINT + "/{demandId}/offers", otherDemand.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_return_400_when_seller_tries_to_create_demand() throws Exception {
    authenticateSeller();

    mockMvc
        .perform(
            multipart(DEMANDS_ENDPOINT)
                .param("description", TEST_DESCRIPTION)
                .param("part.name", TEST_PART_NAME)
                .param("part.carBrand", TEST_CAR_BRAND)
                .param("part.carModel", TEST_CAR_MODEL)
                .param("part.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("part.partCategory", PartCategory.FOG_LIGHTS.name())
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
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

  private void authenticateSeller() {
    val authentication =
        new UsernamePasswordAuthenticationToken(
            "supabase-seller-123",
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(authentication);
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
            .price(150.00)
            .partImageBuckets(new ArrayList<>())
            .build();

    val offer =
        JOffer.builder()
            .id("offer-" + currentTimeMillis())
            .demand(demand)
            .description("Offering part")
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

  private void authenticateUser() {
    val authentication =
        new UsernamePasswordAuthenticationToken(
            DemandControllerIT.TEST_SUPABASE_USER_ID,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
