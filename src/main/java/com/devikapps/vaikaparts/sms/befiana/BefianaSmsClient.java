package com.devikapps.vaikaparts.sms.befiana;

import static com.devikapps.vaikaparts.sms.SmsSendException.Reason.*;

import com.devikapps.vaikaparts.sms.SmsMessage;
import com.devikapps.vaikaparts.sms.SmsProvider;
import com.devikapps.vaikaparts.sms.SmsReceipt;
import com.devikapps.vaikaparts.sms.SmsSendException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
/** Small HTTP adapter for BEFIANA's immediate, single-recipient endpoint. No automatic retries. */
@Slf4j
public final class BefianaSmsClient implements SmsProvider, AutoCloseable {
  private final HttpClient httpClient;
  private final ObjectMapper mapper;
  private final URI endpoint;
  private final String apiKey;
  private final Duration timeout;

  public BefianaSmsClient(
      HttpClient httpClient, ObjectMapper mapper, String baseUrl, String apiKey, Duration timeout) {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalArgumentException("BEFIANA_SMS_BASE_URL is required when SMS is enabled");
    }
    var base = URI.create(baseUrl);
    boolean localHttp = "http".equals(base.getScheme())
        && ("localhost".equals(base.getHost()) || "127.0.0.1".equals(base.getHost()));
    if ((!"https".equals(base.getScheme()) && !localHttp) || base.getHost() == null
        || base.getRawUserInfo() != null || base.getRawQuery() != null || base.getRawFragment() != null
        || !(base.getPath().isEmpty() || "/".equals(base.getPath()))) {
      throw new IllegalArgumentException("BEFIANA_SMS_BASE_URL must be an HTTPS origin (HTTP allowed only on loopback for tests)");
    }
    if (apiKey == null || apiKey.isBlank() || apiKey.contains("\r") || apiKey.contains("\n")) {
      throw new IllegalArgumentException("A valid BEFIANA_SMS_API_KEY is required");
    }
    if (timeout == null || timeout.isZero() || timeout.isNegative()) {
      throw new IllegalArgumentException("SMS request timeout must be positive");
    }
    if (httpClient.followRedirects() != HttpClient.Redirect.NEVER) {
      throw new IllegalArgumentException("SMS HTTP redirects must be disabled to protect the API key");
    }
    this.httpClient = httpClient;
    this.mapper = mapper;
    this.endpoint = base.resolve("/api/smsko/v1/send/");
    this.apiKey = apiKey;
    this.timeout = timeout;
  }

  @Override
  public SmsReceipt send(SmsMessage message) {
    if (message == null || message.message() == null || message.message().isBlank()
        || message.message().codePointCount(0, message.message().length()) > 320) {
      throw new SmsSendException(INVALID_REQUEST, null);
    }
    var phoneNumber = normalizePhoneNumber(message.phoneNumber());
    log.warn("[NOTIF-PIPELINE] SMS message number {}", phoneNumber);
    final String body;
    try {
      body = mapper.writeValueAsString(Map.of("phone_number", phoneNumber, "message", message.message()));
    } catch (JsonProcessingException e) {
      throw new SmsSendException(INVALID_REQUEST, null);
    }
    var request = HttpRequest.newBuilder(endpoint)
        .timeout(timeout)
        .header("Authorization", apiKey)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
        .build();
    final HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SmsSendException(UNKNOWN_OUTCOME, null);
    } catch (IOException e) {
      // The provider may have accepted the SMS before the connection failed.
      throw new SmsSendException(UNKNOWN_OUTCOME, null);
    }
    int status = response.statusCode();
    if (status < 200 || status >= 300) {
      log.error(
          "[BEFIANA-SMS-ERROR] status={}, body={}",
          status,
          formatResponseBody(response.body()));
      var reason = switch (status) {
        case 400 -> INVALID_REQUEST;
        case 401 -> AUTHENTICATION;
        case 403 -> ACCOUNT_UNAVAILABLE;
        case 429 -> RATE_LIMIT;
        default -> PROVIDER_ERROR;
      };
      throw new SmsSendException(reason, status);
    }
    try {
      var json = mapper.readTree(response.body());
      if (json == null || json.has("error") || !json.path("clientCorrelator").isTextual()
          || json.path("clientCorrelator").asText().isBlank()
          || !json.path("address").isTextual() || json.path("address").asText().isBlank()) {
        throw new SmsSendException(UNKNOWN_OUTCOME, status);
      }
      return new SmsReceipt(json.path("clientCorrelator").asText());
    } catch (JsonProcessingException e) {
      throw new SmsSendException(UNKNOWN_OUTCOME, status);
    }
  }

  private String formatResponseBody(String body) {
    if (body == null || body.isBlank()) return body;
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(body));
    } catch (JsonProcessingException e) {
      return body;
    }
  }

  private String normalizePhoneNumber(String value) {
    if (value == null) throw new SmsSendException(INVALID_REQUEST, null);
    var phone = value.replaceAll("[\\s()\\-]", "");
    if (phone.startsWith("+261")) phone = phone.substring(4);
    else if (phone.startsWith("00261")) phone = phone.substring(5);
    else if (phone.startsWith("261")) phone = phone.substring(3);
    if (phone.startsWith("0")) phone = phone.substring(1);
    if (!phone.matches("3[0-9]{8}")) throw new SmsSendException(INVALID_REQUEST, null);
    return phone;
  }

  @Override
  public void close() {
    httpClient.close();
  }
}
