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

class OfferControllerIT extends FacadeIT {

  private static final String OFFERS_ENDPOINT = "/v1/offers";
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

  @Autowired private MockMvc mockMvc;
  @Autowired private OfferRepository offerRepository;
  @Autowired private DemandRepository demandRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ValueObjectMapper vom;

  private JSeller testSeller;
  private JResearcher testResearcher;
  private JDemand testDemand;

  @BeforeEach
  void setUp() {
    testSeller = createTestSeller();
    testResearcher = createTestResearcher();
    testDemand = createTestDemand(testResearcher, PostStatus.PUBLISHED);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    offerRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void should_create_offer_successfully() throws Exception {
    authenticateSeller();

    mockMvc
        .perform(
            multipart(OFFERS_ENDPOINT)
                .param("demandId", testDemand.getId())
                .param("description", TEST_DESCRIPTION)
                .param("partInfo.name", TEST_PART_NAME)
                .param("partInfo.carBrand", TEST_CAR_BRAND)
                .param("partInfo.carModel", TEST_CAR_MODEL)
                .param("partInfo.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("partInfo.partCategory", PartCategory.FOG_LIGHTS.name())
                .param("partInfo.condition", PartCondition.USED.name())
                .param("partInfo.price", String.valueOf(TEST_PRICE))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
        .andExpect(jsonPath("$.status").value(PostStatus.DRAFT.name()))
        .andExpect(jsonPath("$.seller_id").value(TEST_SELLER_ID))
        .andExpect(jsonPath("$.demand.id").value(testDemand.getId()))
        .andExpect(jsonPath("$.parts_info.part.name").value(TEST_PART_NAME))
        .andExpect(jsonPath("$.parts_info.part.car_brand").value(TEST_CAR_BRAND))
        .andExpect(jsonPath("$.parts_info.part.car_model").value(TEST_CAR_MODEL))
        .andExpect(jsonPath("$.parts_info.part.car_year").value(TEST_CAR_YEAR))
        .andExpect(
            jsonPath("$.parts_info.part.part_category").value(PartCategory.FOG_LIGHTS.name()))
        .andExpect(jsonPath("$.parts_info.condition").value(PartCondition.USED.name()))
        .andExpect(jsonPath("$.parts_info.price").value(TEST_PRICE))
        .andExpect(jsonPath("$.parts_info.part.image_urls").isArray())
        .andExpect(jsonPath("$.parts_info.part.image_urls", hasSize(0)))
        .andExpect(jsonPath("$.created_at").isNotEmpty())
        .andExpect(jsonPath("$.updated_at").isNotEmpty())
        .andExpect(jsonPath("$.canceled_at").doesNotExist())
        .andExpect(jsonPath("$.suspended_at").doesNotExist());
  }

  @Test
  void should_create_offer_with_single_image() throws Exception {
    authenticateSeller();

    val imageFile =
        new MockMultipartFile(
            "partInfo.images", "part.jpg", "image/jpeg", "fake image content".getBytes());

    mockMvc
        .perform(
            multipart(OFFERS_ENDPOINT)
                .file(imageFile)
                .param("demandId", testDemand.getId())
                .param("description", TEST_DESCRIPTION)
                .param("partInfo.name", TEST_PART_NAME)
                .param("partInfo.carBrand", TEST_CAR_BRAND)
                .param("partInfo.carModel", TEST_CAR_MODEL)
                .param("partInfo.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("partInfo.partCategory", PartCategory.FOG_LIGHTS.name())
                .param("partInfo.condition", PartCondition.USED.name())
                .param("partInfo.price", String.valueOf(TEST_PRICE))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
        .andExpect(jsonPath("$.parts_info.part.image_urls").isArray())
        .andExpect(jsonPath("$.parts_info.part.image_urls", hasSize(1)))
        .andExpect(jsonPath("$.parts_info.part.image_urls[0]").isString());
  }

  @Test
  void should_create_offer_with_multiple_images() throws Exception {
    authenticateSeller();

    val imageFile1 =
        new MockMultipartFile(
            "partInfo.images", "part-front.jpg", "image/jpeg", "content 1".getBytes());
    val imageFile2 =
        new MockMultipartFile(
            "partInfo.images", "part-side.jpg", "image/jpeg", "content 2".getBytes());
    val imageFile3 =
        new MockMultipartFile(
            "partInfo.images", "part-back.jpg", "image/jpeg", "content 3".getBytes());

    mockMvc
        .perform(
            multipart(OFFERS_ENDPOINT)
                .file(imageFile1)
                .file(imageFile2)
                .file(imageFile3)
                .param("demandId", testDemand.getId())
                .param("description", TEST_DESCRIPTION)
                .param("partInfo.name", TEST_PART_NAME)
                .param("partInfo.carBrand", TEST_CAR_BRAND)
                .param("partInfo.carModel", TEST_CAR_MODEL)
                .param("partInfo.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("partInfo.partCategory", PartCategory.FOG_LIGHTS.name())
                .param("partInfo.condition", PartCondition.USED.name())
                .param("partInfo.price", String.valueOf(TEST_PRICE))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.parts_info.part.image_urls").isArray())
        .andExpect(jsonPath("$.parts_info.part.image_urls", hasSize(3)));
  }

  @Test
  void should_return_400_when_description_is_blank() throws Exception {
    authenticateSeller();

    mockMvc
        .perform(
            multipart(OFFERS_ENDPOINT)
                .param("demandId", testDemand.getId())
                .param("description", "")
                .param("partInfo.name", TEST_PART_NAME)
                .param("partInfo.carBrand", TEST_CAR_BRAND)
                .param("partInfo.carModel", TEST_CAR_MODEL)
                .param("partInfo.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("partInfo.partCategory", PartCategory.FOG_LIGHTS.name())
                .param("partInfo.condition", PartCondition.USED.name())
                .param("partInfo.price", String.valueOf(TEST_PRICE))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_400_when_part_info_name_is_blank() throws Exception {
    authenticateSeller();

    mockMvc
        .perform(
            multipart(OFFERS_ENDPOINT)
                .param("demandId", testDemand.getId())
                .param("description", TEST_DESCRIPTION)
                .param("partInfo.name", "")
                .param("partInfo.carBrand", TEST_CAR_BRAND)
                .param("partInfo.carModel", TEST_CAR_MODEL)
                .param("partInfo.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("partInfo.partCategory", PartCategory.FOG_LIGHTS.name())
                .param("partInfo.condition", PartCondition.USED.name())
                .param("partInfo.price", String.valueOf(TEST_PRICE))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_400_when_price_is_negative() throws Exception {
    authenticateSeller();

    mockMvc
        .perform(
            multipart(OFFERS_ENDPOINT)
                .param("demandId", testDemand.getId())
                .param("description", TEST_DESCRIPTION)
                .param("partInfo.name", TEST_PART_NAME)
                .param("partInfo.carBrand", TEST_CAR_BRAND)
                .param("partInfo.carModel", TEST_CAR_MODEL)
                .param("partInfo.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("partInfo.partCategory", PartCategory.FOG_LIGHTS.name())
                .param("partInfo.condition", PartCondition.USED.name())
                .param("partInfo.price", "-10.0")
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_404_when_demand_not_found() throws Exception {
    authenticateSeller();

    mockMvc
        .perform(
            multipart(OFFERS_ENDPOINT)
                .param("demandId", "non-existent-demand")
                .param("description", TEST_DESCRIPTION)
                .param("partInfo.name", TEST_PART_NAME)
                .param("partInfo.carBrand", TEST_CAR_BRAND)
                .param("partInfo.carModel", TEST_CAR_MODEL)
                .param("partInfo.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("partInfo.partCategory", PartCategory.FOG_LIGHTS.name())
                .param("partInfo.condition", PartCondition.USED.name())
                .param("partInfo.price", String.valueOf(TEST_PRICE))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_return_400_when_demand_is_not_published() throws Exception {
    authenticateSeller();
    val draftDemand = createTestDemand(testResearcher, PostStatus.DRAFT);

    mockMvc
        .perform(
            multipart(OFFERS_ENDPOINT)
                .param("demandId", draftDemand.getId())
                .param("description", TEST_DESCRIPTION)
                .param("partInfo.name", TEST_PART_NAME)
                .param("partInfo.carBrand", TEST_CAR_BRAND)
                .param("partInfo.carModel", TEST_CAR_MODEL)
                .param("partInfo.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("partInfo.partCategory", PartCategory.FOG_LIGHTS.name())
                .param("partInfo.condition", PartCondition.USED.name())
                .param("partInfo.price", String.valueOf(TEST_PRICE))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_400_when_seller_already_has_offer_for_demand() throws Exception {
    authenticateSeller();
    createPersistedOffer(testSeller, testDemand, PostStatus.DRAFT);

    mockMvc
        .perform(
            multipart(OFFERS_ENDPOINT)
                .param("demandId", testDemand.getId())
                .param("description", TEST_DESCRIPTION)
                .param("partInfo.name", TEST_PART_NAME)
                .param("partInfo.carBrand", TEST_CAR_BRAND)
                .param("partInfo.carModel", TEST_CAR_MODEL)
                .param("partInfo.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("partInfo.partCategory", PartCategory.FOG_LIGHTS.name())
                .param("partInfo.condition", PartCondition.USED.name())
                .param("partInfo.price", String.valueOf(TEST_PRICE))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_401_when_not_authenticated() throws Exception {
    mockMvc
        .perform(
            multipart(OFFERS_ENDPOINT)
                .param("demandId", testDemand.getId())
                .param("description", TEST_DESCRIPTION)
                .param("partInfo.name", TEST_PART_NAME)
                .param("partInfo.carBrand", TEST_CAR_BRAND)
                .param("partInfo.carModel", TEST_CAR_MODEL)
                .param("partInfo.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("partInfo.partCategory", PartCategory.FOG_LIGHTS.name())
                .param("partInfo.condition", PartCondition.USED.name())
                .param("partInfo.price", String.valueOf(TEST_PRICE))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void should_return_403_when_researcher_tries_to_create_offer() throws Exception {
    authenticateResearcher();

    mockMvc
        .perform(
            multipart(OFFERS_ENDPOINT)
                .param("demandId", testDemand.getId())
                .param("description", TEST_DESCRIPTION)
                .param("partInfo.name", TEST_PART_NAME)
                .param("partInfo.carBrand", TEST_CAR_BRAND)
                .param("partInfo.carModel", TEST_CAR_MODEL)
                .param("partInfo.carYear", String.valueOf(TEST_CAR_YEAR))
                .param("partInfo.partCategory", PartCategory.FOG_LIGHTS.name())
                .param("partInfo.condition", PartCondition.USED.name())
                .param("partInfo.price", String.valueOf(TEST_PRICE))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_update_offer_status_to_published() throws Exception {
    authenticateSeller();
    val offer = createPersistedOffer(testSeller, testDemand, PostStatus.DRAFT);

    mockMvc
        .perform(
            patch(OFFERS_ENDPOINT + "/{offerId}/status", offer.getId())
                .param("status", PostStatus.PUBLISHED.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(offer.getId()))
        .andExpect(jsonPath("$.status").value(PostStatus.PUBLISHED.name()))
        .andExpect(jsonPath("$.canceled_at").doesNotExist())
        .andExpect(jsonPath("$.suspended_at").doesNotExist());
  }

  @Test
  void should_update_offer_status_to_canceled() throws Exception {
    authenticateSeller();
    val offer = createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);

    mockMvc
        .perform(
            patch(OFFERS_ENDPOINT + "/{offerId}/status", offer.getId())
                .param("status", PostStatus.CANCELED.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PostStatus.CANCELED.name()))
        .andExpect(jsonPath("$.canceled_at").isNotEmpty());
  }

  @Test
  void should_update_offer_status_to_suspended() throws Exception {
    authenticateSeller();
    val offer = createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);

    mockMvc
        .perform(
            patch(OFFERS_ENDPOINT + "/{offerId}/status", offer.getId())
                .param("status", PostStatus.SUSPENDED.name()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PostStatus.SUSPENDED.name()))
        .andExpect(jsonPath("$.suspended_at").isNotEmpty());
  }

  @Test
  void should_return_400_when_updating_to_same_status() throws Exception {
    authenticateSeller();
    val offer = createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);

    mockMvc
        .perform(
            patch(OFFERS_ENDPOINT + "/{offerId}/status", offer.getId())
                .param("status", PostStatus.PUBLISHED.name()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_400_when_updating_canceled_offer() throws Exception {
    authenticateSeller();
    val offer = createPersistedOffer(testSeller, testDemand, PostStatus.CANCELED);

    mockMvc
        .perform(
            patch(OFFERS_ENDPOINT + "/{offerId}/status", offer.getId())
                .param("status", PostStatus.PUBLISHED.name()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_return_404_when_updating_non_existent_offer() throws Exception {
    authenticateSeller();

    mockMvc
        .perform(
            patch(OFFERS_ENDPOINT + "/{offerId}/status", "non-existent-id")
                .param("status", PostStatus.PUBLISHED.name()))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_return_404_when_updating_other_seller_offer() throws Exception {
    authenticateSeller();
    val otherSeller = createOtherSeller();
    val otherOffer = createPersistedOffer(otherSeller, testDemand, PostStatus.DRAFT);

    mockMvc
        .perform(
            patch(OFFERS_ENDPOINT + "/{offerId}/status", otherOffer.getId())
                .param("status", PostStatus.PUBLISHED.name()))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_get_seller_offers_by_status() throws Exception {
    authenticateSeller();
    createPersistedOffer(testSeller, testDemand, PostStatus.DRAFT);
    createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);
    createPersistedOffer(testSeller, testDemand, PostStatus.DRAFT);

    mockMvc
        .perform(
            get(OFFERS_ENDPOINT)
                .param("status", PostStatus.DRAFT.name())
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].status").value(PostStatus.DRAFT.name()))
        .andExpect(jsonPath("$.content[1].status").value(PostStatus.DRAFT.name()))
        .andExpect(jsonPath("$.content[0].seller_id").value(TEST_SELLER_ID))
        .andExpect(jsonPath("$.total_elements").value(2));
  }

  @Test
  void should_get_all_seller_offers() throws Exception {
    authenticateSeller();
    createPersistedOffer(testSeller, testDemand, PostStatus.DRAFT);
    createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);
    createPersistedOffer(testSeller, testDemand, PostStatus.CANCELED);

    mockMvc
        .perform(get(OFFERS_ENDPOINT).param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.total_elements").value(3));
  }

  @Test
  void should_get_all_seller_offers_without_pagination_params() throws Exception {
    authenticateSeller();
    createPersistedOffer(testSeller, testDemand, PostStatus.DRAFT);
    createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);

    mockMvc
        .perform(get(OFFERS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  void should_not_return_other_seller_offers() throws Exception {
    authenticateSeller();
    val otherSeller = createOtherSeller();
    createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);
    createPersistedOffer(otherSeller, testDemand, PostStatus.PUBLISHED);

    mockMvc
        .perform(get(OFFERS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].seller_id").value(TEST_SELLER_ID));
  }

  @Test
  void should_get_offer_by_id() throws Exception {
    authenticateSeller();
    val offer = createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);

    mockMvc
        .perform(get(OFFERS_ENDPOINT + "/{offerId}", offer.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(offer.getId()))
        .andExpect(jsonPath("$.description").value(TEST_DESCRIPTION))
        .andExpect(jsonPath("$.seller_id").value(TEST_SELLER_ID))
        .andExpect(jsonPath("$.demand.id").value(testDemand.getId()))
        .andExpect(jsonPath("$.parts_info.part.name").value(TEST_PART_NAME))
        .andExpect(jsonPath("$.parts_info.price").value(TEST_PRICE))
        .andExpect(jsonPath("$.parts_info.condition").value(PartCondition.USED.name()));
  }

  @Test
  void should_return_404_when_getting_non_existent_offer() throws Exception {
    authenticateSeller();

    mockMvc
        .perform(get(OFFERS_ENDPOINT + "/{offerId}", "non-existent-id"))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_return_404_when_getting_other_seller_offer() throws Exception {
    authenticateSeller();
    val otherSeller = createOtherSeller();
    val otherOffer = createPersistedOffer(otherSeller, testDemand, PostStatus.PUBLISHED);

    mockMvc
        .perform(get(OFFERS_ENDPOINT + "/{offerId}", otherOffer.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_get_offers_by_demand_id() throws Exception {
    authenticateSeller();
    val otherSeller = createOtherSeller();
    val secondDemand = createTestDemand(testResearcher, PostStatus.PUBLISHED);

    createPersistedOffer(testSeller, testDemand, PostStatus.PUBLISHED);
    createPersistedOffer(otherSeller, testDemand, PostStatus.PUBLISHED);
    createPersistedOffer(testSeller, secondDemand, PostStatus.PUBLISHED);

    mockMvc
        .perform(
            get(OFFERS_ENDPOINT + "/demand/{demandId}", testDemand.getId())
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].demand.id").value(testDemand.getId()))
        .andExpect(jsonPath("$.content[1].demand.id").value(testDemand.getId()))
        .andExpect(jsonPath("$.total_elements").value(2));
  }

  @Test
  void should_return_empty_page_when_no_offers_for_demand() throws Exception {
    authenticateSeller();

    mockMvc
        .perform(
            get(OFFERS_ENDPOINT + "/demand/{demandId}", testDemand.getId())
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content", hasSize(0)))
        .andExpect(jsonPath("$.total_elements").value(0));
  }

  @Test
  void should_return_401_when_getting_offers_without_authentication() throws Exception {
    mockMvc.perform(get(OFFERS_ENDPOINT)).andExpect(status().isUnauthorized());
  }

  @Test
  void should_handle_pagination_correctly() throws Exception {
    authenticateSeller();
    for (int i = 0; i < 25; i++) {
      val demand = createTestDemand(testResearcher, PostStatus.PUBLISHED);
      createPersistedOffer(testSeller, demand, PostStatus.PUBLISHED);
    }

    mockMvc
        .perform(
            get(OFFERS_ENDPOINT)
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
            get(OFFERS_ENDPOINT)
                .param("status", PostStatus.PUBLISHED.name())
                .param("page", "2")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(5)))
        .andExpect(jsonPath("$.first").value(false))
        .andExpect(jsonPath("$.last").value(true));
  }

  private void authenticateSeller() {
    val authentication =
        new UsernamePasswordAuthenticationToken(
            TEST_SELLER_SUPABASE_ID,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void authenticateResearcher() {
    val authentication =
        new UsernamePasswordAuthenticationToken(
            TEST_RESEARCHER_SUPABASE_ID,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(authentication);
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
            .supabaseUserId("other-supabase-seller-" + currentTimeMillis())
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
            .id(TEST_RESEARCHER_ID)
            .supabaseUserId(TEST_RESEARCHER_SUPABASE_ID)
            .name("John Researcher")
            .email("researcher@gmail.com")
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

  private JOffer createPersistedOffer(JSeller seller, JDemand demand, PostStatus status) {
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
