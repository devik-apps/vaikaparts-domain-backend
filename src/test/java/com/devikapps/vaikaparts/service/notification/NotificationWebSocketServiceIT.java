package com.devikapps.vaikaparts.service.notification;

import static com.devikapps.vaikaparts.service.notification.NotificationWebSocketService.TOPIC_NOTIFICATIONS_ENDPOINT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.PartCategory;
import com.devikapps.vaikaparts.model.classifier.PostStatus;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.notification.DemandPublishedNotification;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.exchange.JDemand;
import com.devikapps.vaikaparts.repository.model.exchange.JPart;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

@Slf4j
class NotificationWebSocketServiceIT extends FacadeIT {

  private static final String TEST_SELLER_ID = "seller-ws-123";
  private static final String TEST_SELLER_SUPABASE_ID = "supabase-seller-ws-123";
  private static final String TEST_SELLER_NAME = "John Seller";
  private static final String TEST_SELLER_EMAIL = "seller-ws@test.com";
  private static final String TEST_SELLER_PHONE = "+1234567890";

  private static final String TEST_RESEARCHER_ID = "researcher-ws-123";
  private static final String TEST_RESEARCHER_SUPABASE_ID = "supabase-researcher-ws-123";
  private static final String TEST_RESEARCHER_NAME = "Jane Researcher";
  private static final String TEST_RESEARCHER_EMAIL = "researcher-ws@test.com";

  private static final String TEST_DEMAND_ID = "demand-ws-123";
  private static final String TEST_NOTIFICATION_ID = "notification-ws-123";
  private static final String TEST_MESSAGE = "New demand: Toyota Corolla Headlight (2015)";
  private static final String TEST_CLICK_ACTION =
      "{\"action\":\"VIEW_DEMAND\",\"demandId\":\"demand-ws-123\"}";
  private static final int TIMEOUT_SECONDS = 5;

  @Autowired private NotificationWebSocketService notificationWebSocketService;
  @Autowired private UserRepository userRepository;
  @Autowired private DemandRepository demandRepository;
  @Autowired private ValueObjectMapper vom;

  @Autowired private ObjectMapper objectMapper;
  @LocalServerPort private int port;

  private WebSocketStompClient stompClient;
  private JResearcher testResearcher;

  @BeforeEach
  void setUp() {
    testResearcher = createTestResearcher();
    JSeller testSeller = createTestSeller();
    JDemand testDemand = createTestDemand();

    var converter = new MappingJackson2MessageConverter();
    converter.setObjectMapper(objectMapper);

    stompClient =
        new WebSocketStompClient(
            new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
    stompClient.setMessageConverter(converter);
  }

  @AfterEach
  void tearDown() {
    stompClient.stop();
    demandRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void should_deliver_notification_to_correct_seller_topic() throws Exception {
    BlockingQueue<DemandPublishedNotification> queue = new LinkedBlockingQueue<>();
    var notification = buildTestNotification();
    var destination = TOPIC_NOTIFICATIONS_ENDPOINT + TEST_SELLER_ID;

    var session = connectAndSubscribe(destination, queue);

    authenticateSeller();
    notificationWebSocketService.sendNotificationToSeller(TEST_SELLER_ID, notification);

    var received = queue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);

    assertNotNull(received, "Expected to receive a WebSocket notification but got none");
    assertEquals(TEST_NOTIFICATION_ID, received.getId());
    assertEquals(TEST_MESSAGE, received.getMessage());
    assertEquals(TEST_SELLER_ID, received.getSeller().getId());
    assertEquals(TEST_DEMAND_ID, received.getDemand().getId());
    assertEquals(NotificationType.DEMAND_PUBLISHED, received.getNotificationType());
    assertFalse(received.isRead());
    assertEquals(TEST_CLICK_ACTION, received.getClickAction());
    assertNotNull(received.getCreatedAt());
    assertNull(received.getReadAt());

    session.disconnect();
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_not_deliver_notification_to_different_seller_topic() throws Exception {
    BlockingQueue<DemandPublishedNotification> otherSellerQueue = new LinkedBlockingQueue<>();
    var otherSellerId = "other-seller-ws-456";
    var otherDestination = TOPIC_NOTIFICATIONS_ENDPOINT + otherSellerId;
    var notification = buildTestNotification();

    var session = connectAndSubscribe(otherDestination, otherSellerQueue);

    authenticateSeller();
    notificationWebSocketService.sendNotificationToSeller(TEST_SELLER_ID, notification);

    var received = otherSellerQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);

    assertNull(received, "Expected no notification on other seller's topic but received one");

    session.disconnect();
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_deliver_multiple_notifications_to_correct_sellers() throws Exception {
    BlockingQueue<DemandPublishedNotification> seller1Queue = new LinkedBlockingQueue<>();
    BlockingQueue<DemandPublishedNotification> seller2Queue = new LinkedBlockingQueue<>();

    var seller2 = createAnotherSeller();
    var notification1 = buildTestNotification();
    var notification2 = buildNotificationForSeller(seller2.getId(), "notification-ws-456");

    var session1 = connectAndSubscribe(TOPIC_NOTIFICATIONS_ENDPOINT + TEST_SELLER_ID, seller1Queue);
    var session2 =
        connectAndSubscribe(TOPIC_NOTIFICATIONS_ENDPOINT + seller2.getId(), seller2Queue);

    authenticateSeller();
    notificationWebSocketService.sendNotificationToSeller(TEST_SELLER_ID, notification1);
    notificationWebSocketService.sendNotificationToSeller(seller2.getId(), notification2);

    var received1 = seller1Queue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    var received2 = seller2Queue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);

    // seller 1 receives correct notification
    assertNotNull(received1, "Seller 1 expected notification but got none");
    assertEquals(TEST_NOTIFICATION_ID, received1.getId());
    assertEquals(TEST_SELLER_ID, received1.getSeller().getId());
    assertEquals(NotificationType.DEMAND_PUBLISHED, received1.getNotificationType());
    assertFalse(received1.isRead());

    // seller 2 receives correct notification
    assertNotNull(received2, "Seller 2 expected notification but got none");
    assertEquals("notification-ws-456", received2.getId());
    assertEquals(seller2.getId(), received2.getSeller().getId());
    assertEquals(NotificationType.DEMAND_PUBLISHED, received2.getNotificationType());
    assertFalse(received2.isRead());

    // cross-check: seller 1 did NOT receive seller 2's notification and vice versa
    assertNull(
        seller1Queue.poll(1, TimeUnit.SECONDS),
        "Seller 1 should not receive seller 2's notification");
    assertNull(
        seller2Queue.poll(1, TimeUnit.SECONDS),
        "Seller 2 should not receive seller 1's notification");

    session1.disconnect();
    session2.disconnect();
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_throw_when_no_authenticated_user() {
    SecurityContextHolder.clearContext();
    var notification = buildTestNotification();

    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> notificationWebSocketService.sendNotificationToSeller(TEST_SELLER_ID, notification));
  }

  @Test
  void should_deliver_notification_with_null_click_action() throws Exception {
    BlockingQueue<DemandPublishedNotification> queue = new LinkedBlockingQueue<>();
    var notification =
        DemandPublishedNotification.builder()
            .id(TEST_NOTIFICATION_ID)
            .seller(buildSeller(TEST_SELLER_ID))
            .demand(buildDemand())
            .message(TEST_MESSAGE)
            .notificationType(NotificationType.DEMAND_PUBLISHED)
            .read(false)
            .clickAction(null)
            .createdAt(LocalDateTime.now())
            .build();

    var session = connectAndSubscribe(TOPIC_NOTIFICATIONS_ENDPOINT + TEST_SELLER_ID, queue);

    authenticateSeller();
    notificationWebSocketService.sendNotificationToSeller(TEST_SELLER_ID, notification);

    var received = queue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);

    assertNotNull(received, "Expected to receive notification but got none");
    assertEquals(TEST_NOTIFICATION_ID, received.getId());
    assertNull(received.getClickAction());

    session.disconnect();
    SecurityContextHolder.clearContext();
  }

  private StompSession connectAndSubscribe(
      String destination, BlockingQueue<DemandPublishedNotification> queue) throws Exception {

    String url = "ws://localhost:" + port + "/ws";

    StompSession session =
        stompClient
            .connectAsync(url, new StompSessionHandlerAdapter() {})
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

    session.subscribe(
        destination,
        new StompFrameHandler() {
          @Override
          public @NonNull Type getPayloadType(@NonNull StompHeaders headers) {
            return DemandPublishedNotification.class;
          }

          @Override
          public void handleFrame(@NonNull StompHeaders headers, Object payload) {
            var notification = (DemandPublishedNotification) payload;
            if (!queue.offer(notification)) {
              log.warn("Failed to enqueue WebSocket notification with id={}", notification.getId());
            }
          }
        });

    // small wait to ensure subscription is registered before sending
    Thread.sleep(200);

    return session;
  }

  private void authenticateSeller() {
    SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    val authentication =
        new UsernamePasswordAuthenticationToken(
            TEST_SELLER_SUPABASE_ID,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private DemandPublishedNotification buildTestNotification() {
    return buildNotificationForSeller(TEST_SELLER_ID, TEST_NOTIFICATION_ID);
  }

  private DemandPublishedNotification buildNotificationForSeller(
      String sellerId, String notificationId) {
    return DemandPublishedNotification.builder()
        .id(notificationId)
        .seller(buildSeller(sellerId))
        .demand(buildDemand())
        .message(TEST_MESSAGE)
        .notificationType(NotificationType.DEMAND_PUBLISHED)
        .read(false)
        .clickAction(TEST_CLICK_ACTION)
        .createdAt(LocalDateTime.now())
        .build();
  }

  private Seller buildSeller(String sellerId) {
    return Seller.builder().id(sellerId).build();
  }

  private Demand buildDemand() {
    return Demand.builder().id(NotificationWebSocketServiceIT.TEST_DEMAND_ID).build();
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
            .id("other-seller-ws-456")
            .supabaseUserId("supabase-other-seller-ws-456")
            .name("Other Seller")
            .email("other-seller-ws@test.com")
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
            .name(TEST_RESEARCHER_NAME)
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
            .id("part-ws-123")
            .partName("Headlight")
            .carBrand("Toyota")
            .carModel("Corolla")
            .carYear(2015)
            .partCategory(PartCategory.FOG_LIGHTS)
            .imageBuckets(new ArrayList<>())
            .build();

    val demand =
        JDemand.builder()
            .id(TEST_DEMAND_ID)
            .description("Looking for Toyota Corolla headlight")
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
