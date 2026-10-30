package com.devikapps.vaikaparts.service.notification.push;

import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.service.notification.NotificationMessageResolver;
import com.devikapps.vaikaparts.service.notification.WebPushProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

public final class PushDispatchClient {
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
  private static final String AUTHORIZATION = "Authorization";
  private static final Pattern SAFE_BEARER_TOKEN =
      Pattern.compile("[A-Za-z0-9._~+/-]{32,}={0,}");

  private final WebPushProperties properties;
  private final NotificationMessageResolver messageResolver;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final URI dispatchUri;

  public PushDispatchClient(
      WebPushProperties properties,
      NotificationMessageResolver messageResolver,
      ObjectMapper objectMapper) {
    this(
        properties,
        messageResolver,
        objectMapper,
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build());
  }

  PushDispatchClient(
      WebPushProperties properties,
      NotificationMessageResolver messageResolver,
      ObjectMapper objectMapper,
      HttpClient httpClient) {
    this.properties = properties;
    this.messageResolver = messageResolver;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
    this.dispatchUri = validateConfiguration(properties);
    if (httpClient.followRedirects() != HttpClient.Redirect.NEVER) {
      throw new IllegalArgumentException("Push dispatch redirects must be disabled");
    }
  }

  public void dispatch(Notification notification) {
    var payload = PushDispatchRequest.from(notification, messageResolver);
    var secret = properties.getDispatchSecret();
    HttpRequest request;
    try {
      request =
          HttpRequest.newBuilder(dispatchUri)
              .timeout(REQUEST_TIMEOUT)
              .header(AUTHORIZATION, "Bearer " + secret)
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
              .build();
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to encode web push dispatch");
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("Unable to build web push dispatch request");
    }

    try {
      var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException(
            "Next.js push dispatch returned HTTP " + response.statusCode());
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Next.js push dispatch interrupted");
    } catch (IOException exception) {
      throw new IllegalStateException("Next.js push dispatch failed");
    }
  }

  private static URI validateConfiguration(WebPushProperties properties) {
    var secret = properties.getDispatchSecret();
    if (secret == null || !SAFE_BEARER_TOKEN.matcher(secret).matches()) {
      throw new IllegalArgumentException("PUSH_DISPATCH_SECRET must contain at least 32 characters");
    }

    try {
      var uri = URI.create(properties.getDispatchUrl());
      boolean secure = "https".equalsIgnoreCase(uri.getScheme());
      boolean localHttp =
          "http".equalsIgnoreCase(uri.getScheme())
              && ("localhost".equalsIgnoreCase(uri.getHost())
                  || "127.0.0.1".equals(uri.getHost())
                  || "[::1]".equals(uri.getHost()));
      if ((!secure && !localHttp)
          || uri.getHost() == null
          || uri.getRawUserInfo() != null
          || uri.getRawQuery() != null
          || uri.getRawFragment() != null
          || !"/api/push/dispatch".equals(uri.getPath())) {
        throw new IllegalArgumentException();
      }
      return uri;
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException(
          "PUSH_DISPATCH_URL must be HTTPS and point to /api/push/dispatch (HTTP is allowed only on localhost)");
    }
  }
}
