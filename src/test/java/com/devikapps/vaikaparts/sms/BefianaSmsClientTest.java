package com.devikapps.vaikaparts.sms;

import static org.junit.jupiter.api.Assertions.*;

import com.devikapps.vaikaparts.sms.befiana.BefianaSmsClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BefianaSmsClientTest {
  private MockWebServer server;
  private HttpClient httpClient;
  private BefianaSmsClient client;
  private final ObjectMapper mapper = new ObjectMapper();
  private static final String SUCCESS = """
      {"message":"SMS sent successfully.","address":"+261321234567",
       "clientCorrelator":"befiana-test-id","callbackData":"callback"}
      """;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    client = new BefianaSmsClient(httpClient, mapper, server.url("/").toString(),
        "test-secret-key", Duration.ofSeconds(2));
  }

  @AfterEach
  void tearDown() throws Exception {
    httpClient.close();
    server.shutdown();
  }

  @ParameterizedTest
  @ValueSource(strings = {"321234567", "0321234567", "+261321234567", "00261321234567", "032 12 345 67"})
  void sends_expected_request_and_normalizes_phone(String number) throws Exception {
    server.enqueue(new MockResponse().setBody(SUCCESS));
    var receipt = client.send(new SmsMessage(number, "Pièce disponible"));
    assertEquals("befiana-test-id", receipt.providerMessageId());
    var request = server.takeRequest(1, TimeUnit.SECONDS);
    assertNotNull(request);
    assertEquals("POST", request.getMethod());
    assertEquals("/api/smsko/v1/send/", request.getPath());
    assertEquals("test-secret-key", request.getHeader("Authorization"));
    assertEquals("application/json", request.getHeader("Content-Type"));
    var body = mapper.readTree(request.getBody().readUtf8());
    assertEquals(2, body.size());
    assertEquals("321234567", body.get("phone_number").asText());
    assertEquals("Pièce disponible", body.get("message").asText());
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 401, 403, 429, 500, 302})
  void surfaces_status_without_exposing_response_or_retrying(int status) {
    server.enqueue(new MockResponse().setResponseCode(status)
        .setHeader("Location", server.url("/must-not-follow"))
        .setBody("{\"error\":\"test-secret-key 0321234567\"}"));
    var error = assertThrows(SmsSendException.class,
        () -> client.send(new SmsMessage("321234567", "Hello")));
    assertEquals(status, error.getHttpStatus());
    var expected = switch (status) {
      case 400 -> SmsSendException.Reason.INVALID_REQUEST;
      case 401 -> SmsSendException.Reason.AUTHENTICATION;
      case 403 -> SmsSendException.Reason.ACCOUNT_UNAVAILABLE;
      case 429 -> SmsSendException.Reason.RATE_LIMIT;
      default -> SmsSendException.Reason.PROVIDER_ERROR;
    };
    assertEquals(expected, error.getReason());
    assertFalse(error.getMessage().contains("test-secret-key"));
    assertFalse(error.getMessage().contains("0321234567"));
    assertEquals(1, server.getRequestCount());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "{}", "not-json", "{\"error\":\"failure\"}"})
  void does_not_claim_success_for_invalid_success_body(String body) {
    server.enqueue(new MockResponse().setBody(body));
    var error = assertThrows(SmsSendException.class,
        () -> client.send(new SmsMessage("321234567", "Hello")));
    assertEquals(SmsSendException.Reason.UNKNOWN_OUTCOME, error.getReason());
    assertEquals(1, server.getRequestCount());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "123", "+33612345678", "00321234567", "abcdefghij"})
  void rejects_invalid_phone_before_network(String phone) {
    assertThrows(SmsSendException.class, () -> client.send(new SmsMessage(phone, "Hello")));
    assertEquals(0, server.getRequestCount());
  }

  @Test
  void enforces_message_limit_without_silent_truncation() {
    assertThrows(SmsSendException.class, () -> client.send(new SmsMessage("321234567", "a".repeat(321))));
    assertThrows(SmsSendException.class, () -> client.send(new SmsMessage("321234567", " ")));
    assertEquals(0, server.getRequestCount());
    server.enqueue(new MockResponse().setBody(SUCCESS));
    assertNotNull(client.send(new SmsMessage("321234567", "a".repeat(320))));
  }

  @Test
  void timeout_has_unknown_outcome_and_is_not_retried() {
    client = new BefianaSmsClient(httpClient, mapper, server.url("/").toString(),
        "key", Duration.ofMillis(100));
    server.enqueue(new MockResponse().setHeadersDelay(400, TimeUnit.MILLISECONDS).setBody(SUCCESS));
    var error = assertThrows(SmsSendException.class,
        () -> client.send(new SmsMessage("321234567", "Hello")));
    assertEquals(SmsSendException.Reason.UNKNOWN_OUTCOME, error.getReason());
    assertTrue(server.getRequestCount() <= 1);
  }

  @Test
  void rejects_unsafe_or_missing_configuration() {
    for (String base : new String[]{"", "http://provider.example", "https://user:password@provider.example",
        "https://provider.example/path", "https://provider.example?key=secret"}) {
      assertThrows(IllegalArgumentException.class,
          () -> new BefianaSmsClient(httpClient, mapper, base, "key", Duration.ofSeconds(1)));
    }
    assertThrows(IllegalArgumentException.class,
        () -> new BefianaSmsClient(httpClient, mapper, "https://provider.example", "", Duration.ofSeconds(1)));
  }
}
