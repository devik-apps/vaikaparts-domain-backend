package com.devikapps.vaikaparts.service.notification.push;

import static org.junit.jupiter.api.Assertions.*;

import com.devikapps.vaikaparts.config.JacksonConf;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.UserLanguage;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.service.notification.NotificationMessageResolver;
import com.devikapps.vaikaparts.service.notification.WebPushProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PushDispatchClientTest {
  private static final String SECRET = "0123456789abcdef0123456789abcdef";

  private final ObjectMapper mapper = new JacksonConf().objectMapper();
  private final AtomicReference<String> authorization = new AtomicReference<>();
  private final AtomicReference<String> requestBody = new AtomicReference<>();
  private final AtomicInteger targetRequests = new AtomicInteger();
  private final AtomicInteger responseStatus = new AtomicInteger(202);
  private final AtomicReference<String> redirectLocation = new AtomicReference<>();
  private HttpServer server;
  private PushDispatchClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/push/dispatch",
        exchange -> {
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          if (redirectLocation.get() != null) {
            exchange.getResponseHeaders().add("Location", redirectLocation.get());
          }
          exchange.sendResponseHeaders(responseStatus.get(), -1);
          exchange.close();
        });
    server.createContext(
        "/target",
        exchange -> {
          targetRequests.incrementAndGet();
          exchange.sendResponseHeaders(202, -1);
          exchange.close();
        });
    server.start();
    client = clientFor("/api/push/dispatch", SECRET);
  }

  @AfterEach
  void tearDown() {
    if (server != null) server.stop(0);
  }

  @Test
  void dispatchSendsTheNextContractWithSupabaseIdentityAndSharedBearerSecret() throws Exception {
    client.dispatch(notification("A notification body", "/fr/researcher/notifications"));

    assertEquals("Bearer " + SECRET, authorization.get());
    JsonNode body = mapper.readTree(requestBody.get());
    assertEquals("notification-123", body.get("eventId").asText());
    assertEquals("a9c19b5e-8011-45da-9c01-07a0c1278b51", body.get("userId").asText());
    assertEquals("VaikaParts", body.get("title").asText());
    assertEquals("A notification body", body.get("body").asText());
    assertEquals("/fr/researcher/notifications", body.get("url").asText());
    assertEquals(5, body.size());
  }

  @Test
  void dispatchBoundsBodyAndFallsBackForUnsafeClickAction() throws Exception {
    client.dispatch(notification("😀".repeat(200), "https://evil.example/path"));

    JsonNode body = mapper.readTree(requestBody.get());
    assertEquals(300, body.get("body").asText().length());
    assertTrue(body.get("body").asText().endsWith("😀"));
    assertEquals("/notifications", body.get("url").asText());
  }

  @Test
  void duplicateDispatchResponseIsAccepted() {
    responseStatus.set(200);

    assertDoesNotThrow(() -> client.dispatch(notification("Duplicate", "/notifications")));
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 503})
  void nonSuccessResponseFailsWithoutExposingTheSharedSecret(int status) {
    responseStatus.set(status);

    var failure =
        assertThrows(
            IllegalStateException.class,
            () -> client.dispatch(notification("Unavailable", "/notifications")));
    assertFalse(failure.getMessage().contains(SECRET));
  }

  @Test
  void redirectIsRejectedWithoutForwardingTheBearerSecret() {
    responseStatus.set(307);
    redirectLocation.set("/target");

    assertThrows(
        IllegalStateException.class,
        () -> client.dispatch(notification("Redirect", "/notifications")));
    assertEquals(0, targetRequests.get());
  }

  @Test
  void rejectsShortSecretAndNonLocalHttpEndpoint() {
    assertThrows(IllegalArgumentException.class, () -> clientFor("/api/push/dispatch", "short"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PushDispatchClient(
                properties("http://push.example.com/api/push/dispatch", SECRET),
                new NotificationMessageResolver(),
                mapper));
  }

  @Test
  void rejectsBearerSecretsContainingHeaderUnsafeCharactersWithoutEchoingThem() {
    var secret = SECRET + "\u000b";

    var failure =
        assertThrows(
            IllegalArgumentException.class,
            () -> clientFor("/api/push/dispatch", secret));

    assertFalse(failure.getMessage().contains(secret));
  }

  @Test
  void rejectsNonCanonicalSupabaseUserId() {
    var notification = notification("Invalid recipient", "/notifications");
    notification.getRecipient().setSupabaseUserId("1-1-1-1-1");

    assertThrows(IllegalArgumentException.class, () -> client.dispatch(notification));
  }

  private PushDispatchClient clientFor(String path, String secret) {
    return new PushDispatchClient(
        properties("http://127.0.0.1:" + server.getAddress().getPort() + path, secret),
        new NotificationMessageResolver(),
        mapper);
  }

  private WebPushProperties properties(String dispatchUrl, String secret) {
    var properties = new WebPushProperties();
    properties.setDispatchUrl(dispatchUrl);
    properties.setDispatchSecret(secret);
    return properties;
  }

  private Notification notification(String message, String clickAction) {
    return Notification.builder()
        .id("notification-123")
        .notificationType(NotificationType.SYSTEM_ANNOUNCEMENT)
        .message(message)
        .clickAction(clickAction)
        .recipient(
            Researcher.builder()
                .id("java-user-456")
                .supabaseUserId("a9c19b5e-8011-45da-9c01-07a0c1278b51")
                .preferredLanguage(UserLanguage.EN)
                .build())
        .build();
  }
}
