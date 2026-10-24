package com.devikapps.vaikaparts.client;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devikapps.vaikaparts.client.api.NotificationsApi;
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.Notification;
import com.devikapps.vaikaparts.client.model.NotificationPageResponse;
import com.devikapps.vaikaparts.client.model.NotificationType;
import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
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
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class NotificationClientIT extends FacadeIT {

  private static final String BASE_URL = "http://localhost";

  private static final String TEST_SELLER_ID = randomUUID().toString();
  private static final String TEST_SELLER_SUPABASE_ID = randomUUID().toString();
  private static final String TEST_RESEARCHER_ID = randomUUID().toString();
  private static final String TEST_RESEARCHER_SUPABASE_ID = randomUUID().toString();

  private static final String TEST_DESCRIPTION = "Looking for Toyota Corolla headlight";
  private static final String TEST_PART_NAME = "Headlight";
  private static final String TEST_CAR_BRAND = "Toyota";
  private static final String TEST_CAR_MODEL = "Corolla";
  private static final int TEST_CAR_YEAR = 2015;
  private static final String TEST_MESSAGE = "New demand: Toyota Corolla Headlight (2015)";
  private static final String TEST_CLICK_ACTION =
      "{\"action\":\"VIEW_DEMAND\",\"demandId\":\"test-demand\"}";

  @LocalServerPort private int port;

  @Autowired private DemandPublishedNotificationRepository demandPublishedNotificationRepository;
  @Autowired private NotificationRequestedRepository notificationRequestedRepository;
  @Autowired private DemandPublishedRequestedRepository demandPublishedRequestedRepository;
  @Autowired private DemandRepository demandRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ValueObjectMapper vom;

  private NotificationsApi authenticatedSellerClient;
  private NotificationsApi unauthenticatedClient;

  private JSeller testSeller;
  private JDemand testDemand;

  @BeforeEach
  void setUp() {
    val testResearcher = createTestResearcher();
    testSeller = createTestSeller();
    testDemand = createPersistedDemand(testResearcher);

    authenticatedSellerClient = buildClient(JwtTestFactory.generateToken(TEST_SELLER_SUPABASE_ID));
    unauthenticatedClient = buildClient(null);
  }

  @AfterEach
  void tearDown() {
    demandPublishedNotificationRepository.deleteAll();
    notificationRequestedRepository.deleteAll();
    demandPublishedRequestedRepository.deleteAll();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  // -------------------------------------------------------------------------
  // GET /v1/notifications/demand-published
  // -------------------------------------------------------------------------

  @Test
  void fetch_notifications_test() {
    saveNotification(testSeller, testDemand);
    saveNotification(testSeller, testDemand);

    ResponseEntity<NotificationPageResponse> ok =
        authenticatedSellerClient.fetchNotificationsWithHttpInfo(0, 10);

    assertThat(ok.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    NotificationPageResponse page = ok.getBody();
    assertThat(page).isNotNull();
    assertThat(page.getContent()).hasSize(2);
    assertThat(page.getTotalElements()).isEqualTo(2L);
    assertThat(page.getPageable()).isNotNull();

    Notification first = page.getContent().getFirst();
    assertThat(first.getId()).isNotNull();
    assertThat(first.getMessage()).isNotBlank();
    assertThat(first.getRead()).isFalse();
    assertThat(first.getNotificationType()).isEqualTo(NotificationType.DEMAND_PUBLISHED);
    assertThat(first.getCreatedAt()).isNotNull();
    assertThat(first.getRecipient()).isNotNull();
    assertThat(first.getRecipient().getId().toString()).isEqualTo(TEST_SELLER_ID);

    // Empty page — no notifications exist for this seller
    demandPublishedNotificationRepository.deleteAll();

    ResponseEntity<NotificationPageResponse> empty =
        authenticatedSellerClient.fetchNotificationsWithHttpInfo(0, 10);

    assertThat(empty.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
    assertThat(empty.getBody()).isNotNull();
    assertThat(empty.getBody().getContent()).isEmpty();
    assertThat(empty.getBody().getTotalElements()).isZero();

    for (int i = 0; i < 5; i++) {
      saveNotification(testSeller, testDemand);
    }

    ResponseEntity<NotificationPageResponse> paged =
        authenticatedSellerClient.fetchNotificationsWithHttpInfo(0, 2);

    assertThat(paged.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    NotificationPageResponse pagedBody = paged.getBody();
    assertThat(pagedBody).isNotNull();
    assertThat(pagedBody.getContent()).hasSize(2);
    assertThat(pagedBody.getTotalElements()).isEqualTo(5L);
    assertThat(pagedBody.getTotalPages()).isEqualTo(3);
    assertThat(pagedBody.getNumber()).isZero();
    assertThat(pagedBody.getSize()).isEqualTo(2);

    // Isolation — notifications belonging to another seller are never returned
    JSeller otherSeller = createOtherSeller();
    saveNotificationForSeller(otherSeller, testDemand);

    ResponseEntity<NotificationPageResponse> isolated =
        authenticatedSellerClient.fetchNotificationsWithHttpInfo(0, 10);

    assertThat(isolated.getBody()).isNotNull();
    isolated
        .getBody()
        .getContent()
        .forEach(
            n -> {
              assertThat(n.getRecipient()).isNotNull();
              assertThat(n.getRecipient().getId().toString()).isEqualTo(TEST_SELLER_ID);
            });

    // 401 — unauthenticated request is rejected
    assertThatThrownBy(() -> unauthenticatedClient.fetchNotifications(0, 10))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
  }

  // -------------------------------------------------------------------------
  // GET /v1/notifications/demand-published/{notificationId}
  // -------------------------------------------------------------------------

  @Test
  void get_notification_test() {
    JDemandPublishedNotification persisted = saveNotification(testSeller, testDemand);
    var notificationId = fromString(persisted.getId());

    ResponseEntity<Notification> ok =
        authenticatedSellerClient.getNotificationWithHttpInfo(notificationId);

    assertThat(ok.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    Notification notification = ok.getBody();
    assertThat(notification).isNotNull();
    assertThat(notification.getId()).isEqualTo(notificationId);
    assertThat(notification.getMessage()).isEqualTo(TEST_MESSAGE);
    assertThat(notification.getRead()).isFalse();
    assertThat(notification.getReadAt()).isNull();
    assertThat(notification.getNotificationType()).isEqualTo(NotificationType.DEMAND_PUBLISHED);
    assertThat(notification.getCreatedAt()).isNotNull();
    assertThat(notification.getRecipient()).isNotNull();
    assertThat(notification.getRecipient().getId().toString()).isEqualTo(TEST_SELLER_ID);
    assertThat(notification.getResource()).isNotNull();

    // 404 — notification does not exist
    assertThatThrownBy(() -> authenticatedSellerClient.getNotification(randomUUID()))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.NOT_FOUND.value()));

    // 404 — notification belongs to another seller (information disclosure prevention)
    JSeller otherSeller = createOtherSeller();
    JDemandPublishedNotification otherNotification =
        saveNotificationForSeller(otherSeller, testDemand);

    assertThatThrownBy(
            () -> authenticatedSellerClient.getNotification(fromString(otherNotification.getId())))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.NOT_FOUND.value()));

    // 401 — unauthenticated request is rejected
    assertThatThrownBy(() -> unauthenticatedClient.getNotification(notificationId))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
  }

  // -------------------------------------------------------------------------
  // PATCH /v1/notifications/demand-published/mark-as-read/{notificationId}
  // -------------------------------------------------------------------------

  @Test
  void mark_as_read_test() {
    JDemandPublishedNotification persisted = saveNotification(testSeller, testDemand);
    UUID notificationId = fromString(persisted.getId());

    assertThat(persisted.isRead()).isFalse();

    ResponseEntity<Notification> ok =
        authenticatedSellerClient.markAsReadWithHttpInfo(notificationId);

    assertThat(ok.getStatusCode().value()).isEqualTo(HttpStatus.ACCEPTED.value());

    Notification updated = ok.getBody();
    assertThat(updated).isNotNull();
    assertThat(updated.getId()).isEqualTo(notificationId);
    assertThat(updated.getRead()).isTrue();
    assertThat(updated.getReadAt()).isNotNull();
    assertThat(updated.getMessage()).isEqualTo(TEST_MESSAGE);
    assertThat(updated.getRecipient()).isNotNull();
    assertThat(updated.getRecipient().getId().toString()).isEqualTo(TEST_SELLER_ID);

    // Idempotency — marking an already-read notification stays accepted and read remains true
    ResponseEntity<Notification> idempotent =
        authenticatedSellerClient.markAsReadWithHttpInfo(notificationId);

    assertThat(idempotent.getStatusCode().value()).isEqualTo(HttpStatus.ACCEPTED.value());
    assertThat(idempotent.getBody()).isNotNull();
    assertThat(idempotent.getBody().getRead()).isTrue();
    assertThat(idempotent.getBody().getReadAt()).isNotNull();

    // 404 — notification does not exist
    assertThatThrownBy(() -> authenticatedSellerClient.markAsRead(randomUUID()))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.NOT_FOUND.value()));

    // 404 — notification belongs to another seller; their read state must remain unchanged
    JSeller otherSeller = createOtherSeller();
    JDemandPublishedNotification otherNotification =
        saveNotificationForSeller(otherSeller, testDemand);

    assertThatThrownBy(
            () -> authenticatedSellerClient.markAsRead(fromString(otherNotification.getId())))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.NOT_FOUND.value()));

    assertThat(demandPublishedNotificationRepository.findById(otherNotification.getId()))
        .isPresent()
        .hasValueSatisfying(
            n -> {
              assertThat(n.isRead()).isFalse();
              assertThat(n.getReadAt()).isNull();
            });

    // 401 — unauthenticated request is rejected
    JDemandPublishedNotification another = saveNotification(testSeller, testDemand);

    assertThatThrownBy(() -> unauthenticatedClient.markAsRead(fromString(another.getId())))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
  }

  private NotificationsApi buildClient(String bearerToken) {
    RestClient.Builder builder = RestClient.builder().baseUrl(format("%s:%s", BASE_URL, port));

    if (bearerToken != null) builder.defaultHeader("Authorization", "Bearer " + bearerToken);

    ApiClient apiClient = new ApiClient(builder.build());
    apiClient.setBasePath(format("%s:%s", BASE_URL, port));
    return new NotificationsApi(apiClient);
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
            .recipient(seller)
            .demand(demand)
            .message(TEST_MESSAGE)
            .notificationType(
                com.devikapps.vaikaparts.model.classifier.NotificationType.DEMAND_PUBLISHED)
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
                .id(randomUUID().toString())
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
            .id(randomUUID().toString())
            .demandPublishedRequested(dpr)
            .seller(seller)
            .demand(demand)
            .notificationType(
                com.devikapps.vaikaparts.model.classifier.NotificationType.DEMAND_PUBLISHED)
            .status(ProcessStatus.PENDING)
            .attemptNb(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build());
  }

  private JSeller createTestSeller() {
    return userRepository.save(
        JSeller.builder()
            .id(NotificationClientIT.TEST_SELLER_ID)
            .supabaseUserId(NotificationClientIT.TEST_SELLER_SUPABASE_ID)
            .name("Contract Test Seller")
            .email("seller-client-it@test.com")
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
            .email("other-seller-" + randomUUID() + "@test.com")
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
            .id(NotificationClientIT.TEST_RESEARCHER_ID)
            .supabaseUserId(NotificationClientIT.TEST_RESEARCHER_SUPABASE_ID)
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
            .description(TEST_DESCRIPTION)
            .attachedPhotoBucketKeys(new ArrayList<>())
            .part(part)
            .researcher(researcher)
            .status(PostStatus.PUBLISHED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    part.setDemand(demand);
    return demandRepository.save(demand);
  }
}
