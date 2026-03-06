package com.devikapps.vaikaparts.endpoint.rest.controller;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.DemandPublishedRequestedRepository;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.NotificationRequestedRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotificationRequested;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedRequested;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.time.LocalDateTime;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
class NotificationControllerIT extends FacadeIT {

  private static final String BASE_URL = "/v1/notifications/demand-published";

  private static final String TEST_SELLER_ID = "seller-ctrl-123";
  private static final String TEST_SELLER_NAME = "John Seller";
  private static final String TEST_SELLER_EMAIL = "seller-ctrl@test.com";
  private static final String TEST_SELLER_PHONE = "+1234567890";
  private static final String TEST_SELLER_SUPABASE_ID = "supabase-seller-ctrl-123";

  private static final String TEST_RESEARCHER_ID = "researcher-ctrl-456";
  private static final String TEST_RESEARCHER_EMAIL = "researcher-ctrl@test.com";
  private static final String TEST_RESEARCHER_SUPABASE_ID = "supabase-researcher-ctrl-456";

  private static final String TEST_DEMAND_ID = "demand-ctrl-789";
  private static final String TEST_DESCRIPTION = "Looking for Toyota Corolla headlight";
  private static final String TEST_PART_NAME = "Headlight";
  private static final String TEST_CAR_BRAND = "Toyota";
  private static final String TEST_CAR_MODEL = "Corolla";
  private static final int TEST_CAR_YEAR = 2015;
  private static final String TEST_MESSAGE = "New demand: Toyota Corolla Headlight (2015)";
  private static final String TEST_CLICK_ACTION =
      "{\"action\":\"VIEW_DEMAND\",\"demandId\":\"demand-ctrl-789\"}";

  @Autowired private MockMvc mockMvc;
  @Autowired private DemandPublishedNotificationRepository demandPublishedNotificationRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private DemandRepository demandRepository;
  @Autowired private NotificationRequestedRepository notificationRequestedRepository;
  @Autowired private DemandPublishedRequestedRepository demandPublishedRequestedRepository;
  @Autowired private ValueObjectMapper vom;

  private JSeller testSeller;
  private JResearcher testResearcher;
  private JDemand testDemand;

  @BeforeEach
  void setUp() {
    testResearcher = createTestResearcher();
    testSeller = createTestSeller();
    testDemand = createTestDemand();
    authenticateSeller();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    demandPublishedNotificationRepository.deleteAll();
    notificationRequestedRepository.deleteAll();
    demandPublishedRequestedRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void should_fetch_all_notifications_returns_200_with_page() throws Exception {
    saveNotification(testSeller, testDemand);
    saveNotification(testSeller, testDemand);

    mockMvc
        .perform(get(BASE_URL).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total_elements").value(2))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].seller.id").value(testSeller.getId()))
        .andExpect(jsonPath("$.content[0].read").value(false))
        .andExpect(jsonPath("$.content[0].notification_type").value("DEMAND_PUBLISHED"))
        .andExpect(jsonPath("$.content[0].id").isNotEmpty())
        .andExpect(jsonPath("$.content[0].created_at").isNotEmpty());
  }

  @Test
  void should_fetch_notifications_with_pagination_params() throws Exception {
    for (int i = 0; i < 5; i++) {
      saveNotification(testSeller, testDemand);
    }

    mockMvc
        .perform(get(BASE_URL).param("page", "0").param("size", "2").contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total_elements").value(5))
        .andExpect(jsonPath("$.total_pages").value(3))
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.size").value(2));
  }

  @Test
  void should_fetch_empty_page_when_no_notifications() throws Exception {
    mockMvc
        .perform(get(BASE_URL).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total_elements").value(0))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content").isEmpty());
  }

  @Test
  void should_not_return_notifications_of_another_seller() throws Exception {
    val otherSeller = createAnotherSeller();
    saveNotificationForSeller(otherSeller, testDemand);
    saveNotification(testSeller, testDemand);

    mockMvc
        .perform(get(BASE_URL).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total_elements").value(1))
        .andExpect(jsonPath("$.content[0].seller.id").value(testSeller.getId()));
  }

  @Test
  void should_get_notification_by_id_returns_200() throws Exception {
    val saved = saveNotification(testSeller, testDemand);

    mockMvc
        .perform(get(BASE_URL + "/" + saved.getId()).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(saved.getId()))
        .andExpect(jsonPath("$.seller.id").value(testSeller.getId()))
        .andExpect(jsonPath("$.message").value(TEST_MESSAGE))
        .andExpect(jsonPath("$.notification_type").value("DEMAND_PUBLISHED"))
        .andExpect(jsonPath("$.read").value(false))
        .andExpect(jsonPath("$.read_at").doesNotExist())
        .andExpect(jsonPath("$.click_action").value(TEST_CLICK_ACTION))
        .andExpect(jsonPath("$.created_at").isNotEmpty());
  }

  @Test
  void should_return_404_when_notification_not_found() throws Exception {
    mockMvc
        .perform(get(BASE_URL + "/non-existent-id").contentType(APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_return_404_when_notification_belongs_to_another_seller() throws Exception {
    val otherSeller = createAnotherSeller();
    val otherNotification = saveNotificationForSeller(otherSeller, testDemand);

    mockMvc
        .perform(get(BASE_URL + "/" + otherNotification.getId()).contentType(APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_mark_notification_as_read_returns_202() throws Exception {
    val saved = saveNotification(testSeller, testDemand);
    assertFalse(saved.isRead());

    mockMvc
        .perform(patch(BASE_URL + "/mark-as-read/" + saved.getId()).contentType(APPLICATION_JSON))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.id").value(saved.getId()))
        .andExpect(jsonPath("$.read").value(true))
        .andExpect(jsonPath("$.read_at").isNotEmpty())
        .andExpect(jsonPath("$.seller.id").value(testSeller.getId()))
        .andExpect(jsonPath("$.message").value(TEST_MESSAGE));
  }

  @Test
  void should_return_404_when_marking_non_existent_notification_as_read() throws Exception {
    mockMvc
        .perform(patch(BASE_URL + "/mark-as-read/non-existent-id").contentType(APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void should_return_404_when_marking_another_sellers_notification_as_read() throws Exception {
    val otherSeller = createAnotherSeller();
    val otherNotification = saveNotificationForSeller(otherSeller, testDemand);

    mockMvc
        .perform(
            patch(BASE_URL + "/mark-as-read/" + otherNotification.getId())
                .contentType(APPLICATION_JSON))
        .andExpect(status().isNotFound());

    val persisted = demandPublishedNotificationRepository.findById(otherNotification.getId());
    assertTrue(persisted.isPresent());
    assertFalse(persisted.get().isRead());
    assertNull(persisted.get().getReadAt());
  }

  @Test
  void should_mark_already_read_notification_remains_read() throws Exception {
    val saved = saveNotification(testSeller, testDemand);
    mockMvc
        .perform(patch(BASE_URL + "/mark-as-read/" + saved.getId()).contentType(APPLICATION_JSON))
        .andExpect(status().isAccepted());

    mockMvc
        .perform(patch(BASE_URL + "/mark-as-read/" + saved.getId()).contentType(APPLICATION_JSON))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.read").value(true))
        .andExpect(jsonPath("$.read_at").isNotEmpty());
  }

  private void authenticateSeller() {
    val authentication =
        new UsernamePasswordAuthenticationToken(TEST_SELLER_SUPABASE_ID, null, new ArrayList<>());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private JDemandPublishedNotification saveNotification(JSeller seller, JDemand demand) {
    return saveNotificationForSeller(seller, demand);
  }

  private JDemandPublishedNotification saveNotificationForSeller(JSeller seller, JDemand demand) {
    val nr = createNotificationRequested(seller, demand);
    return demandPublishedNotificationRepository.save(
        JDemandPublishedNotification.builder()
            .id(randomUUID().toString())
            .notificationRequested(nr)
            .seller(seller)
            .demand(demand)
            .message(TEST_MESSAGE)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .read(false)
            .clickAction(TEST_CLICK_ACTION)
            .createdAt(LocalDateTime.now())
            .build());
  }

  private JDemandPublishedNotificationRequested createNotificationRequested(
      JSeller seller, JDemand demand) {
    val dpr =
        demandPublishedRequestedRepository.save(
            JDemandPublishedRequested.builder()
                .id("dpr-" + randomUUID())
                .demand(demand)
                .status(ProcessStatus.PENDING)
                .attemptNb(0)
                .totalSellersToNotify(1)
                .notificationsSentCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

    return notificationRequestedRepository.save(
        JDemandPublishedNotificationRequested.builder()
            .id("nr-" + randomUUID())
            .demandPublishedRequested(dpr)
            .seller(seller)
            .demand(demand)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .status(ProcessStatus.PENDING)
            .attemptNb(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build());
  }

  private JSeller createTestSeller() {
    return userRepository.save(
        JSeller.builder()
            .id(TEST_SELLER_ID)
            .supabaseUserId(TEST_SELLER_SUPABASE_ID)
            .name(TEST_SELLER_NAME)
            .email(TEST_SELLER_EMAIL)
            .phoneNumber(TEST_SELLER_PHONE)
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JSeller createAnotherSeller() {
    return userRepository.save(
        JSeller.builder()
            .id("seller-other-" + randomUUID())
            .supabaseUserId("supabase-other-" + randomUUID())
            .name("Other Seller")
            .email("other-" + randomUUID() + "@test.com")
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
            .name("Jane Researcher")
            .email(TEST_RESEARCHER_EMAIL)
            .phoneNumber("+1234567890")
            .profileImgKey("")
            .location(vom.map(Location.getDefault()))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .build());
  }

  private JDemand createTestDemand() {
    val part =
        JPart.builder()
            .id("part-ctrl-" + randomUUID())
            .partName(TEST_PART_NAME)
            .carBrand(TEST_CAR_BRAND)
            .carModel(TEST_CAR_MODEL)
            .carYear(TEST_CAR_YEAR)
            .partCategory(PartCategory.FOG_LIGHTS)
            .imageBuckets(new ArrayList<>())
            .build();

    val demand =
        JDemand.builder()
            .id(TEST_DEMAND_ID)
            .description(TEST_DESCRIPTION)
            .attachedPhotoBucketKeys(new ArrayList<>())
            .part(part)
            .researcher(testResearcher)
            .status(PostStatus.PUBLISHED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    part.setDemand(demand);
    return demandRepository.save(demand);
  }
}
