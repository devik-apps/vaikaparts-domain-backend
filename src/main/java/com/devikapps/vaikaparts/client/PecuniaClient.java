package com.devikapps.vaikaparts.client;

import com.devikapps.vaikaparts.config.PecuniaConf;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PecuniaClient implements AutoCloseable {

  private static final String API_KEY_HEADER = "X-Api-Key";

  private final PecuniaConf conf;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  @Autowired
  public PecuniaClient(PecuniaConf conf, ObjectMapper objectMapper) {
    this(
        conf,
        objectMapper,
        HttpClient.newBuilder()
            .connectTimeout(conf.getConnectTimeout())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build());
  }

  PecuniaClient(PecuniaConf conf, ObjectMapper objectMapper, HttpClient httpClient) {
    this.conf = conf;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
  }

  public PaymentResponse initiate(
      String unlockRequestId,
      String buyerId,
      String sellerId,
      BigDecimal amount,
      String description) {
    var payload =
        new PaymentRequest(
            amount,
            "MGA",
            description,
            "PROFILE_UNLOCK",
            unlockRequestId,
            buyerId,
            sellerId);
    return exchange("/v1/payments/vanilla-pay", "POST", payload);
  }

  public PaymentResponse findByUnlockRequestId(String unlockRequestId) {
    var encoded = URLEncoder.encode(unlockRequestId, StandardCharsets.UTF_8);
    return exchange("/v1/payments/by-unlock-request/" + encoded, "GET", null);
  }

  private PaymentResponse exchange(String path, String method, PaymentRequest payload) {
    validateConfiguration();
    try {
      var builder =
          HttpRequest.newBuilder(resolve(path))
              .timeout(conf.getRequestTimeout())
              .header(API_KEY_HEADER, conf.getApiKey())
              .header("Accept", "application/json");
      if ("POST".equals(method)) {
        builder
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));
      } else {
        builder.GET();
      }

      var response =
          httpClient.send(
              builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new PecuniaClientException("Pecunia returned HTTP " + response.statusCode());
      }
      return objectMapper.readValue(response.body(), PaymentResponse.class);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new PecuniaUnknownOutcomeException("Pecunia request interrupted", e);
    } catch (IOException e) {
      throw new PecuniaUnknownOutcomeException("Pecunia request outcome is unknown", e);
    }
  }

  private URI resolve(String path) {
    var base = URI.create(conf.getBaseUrl());
    if (base.getScheme() == null || base.getHost() == null || base.getRawUserInfo() != null) {
      throw new PecuniaClientException("Invalid Pecunia base URL");
    }
    return base.resolve(path);
  }

  private void validateConfiguration() {
    if (conf.getBaseUrl() == null || conf.getBaseUrl().isBlank()) {
      throw new PecuniaClientException("PECUNIA_BASE_URL is required");
    }
    if (conf.getApiKey() == null
        || conf.getApiKey().isBlank()
        || conf.getApiKey().contains("\r")
        || conf.getApiKey().contains("\n")) {
      throw new PecuniaClientException("A valid PECUNIA_API_KEY is required");
    }
  }

  @Override
  public void close() {
    httpClient.close();
  }

  private record PaymentRequest(
      BigDecimal amount,
      String currency,
      String description,
      String type,
      String unlockRequestId,
      String buyerId,
      String sellerId) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PaymentResponse(
      String paymentId,
      String transactionId,
      String paymentUrl,
      String status,
      BigDecimal amount,
      String currency,
      String description,
      String type,
      String unlockRequestId,
      String buyerId,
      String sellerId,
      String provider) {}
}
