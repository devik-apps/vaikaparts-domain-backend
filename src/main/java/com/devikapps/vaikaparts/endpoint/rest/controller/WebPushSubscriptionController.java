package com.devikapps.vaikaparts.endpoint.rest.controller;

import com.devikapps.vaikaparts.service.notification.WebPushProperties;
import com.devikapps.vaikaparts.service.notification.push.WebPushSubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/notifications/push-subscriptions")
@RequiredArgsConstructor
public class WebPushSubscriptionController {
  private final WebPushSubscriptionService subscriptions;
  private final WebPushProperties properties;

  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void register(@Valid @RequestBody SubscriptionRequest request) {
    subscriptions.register(request.endpoint(), request.keys().p256dh(), request.keys().auth());
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@Valid @RequestBody DeleteRequest request) {
    subscriptions.delete(request.endpoint());
  }

  @GetMapping("/vapid-public-key")
  public ResponseEntity<Map<String, String>> publicKey() {
    if (!properties.isEnabled()
        || properties.getPublicKey() == null
        || properties.getPublicKey().isBlank()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
    return ResponseEntity.ok(Map.of("publicKey", properties.getPublicKey()));
  }

  public record SubscriptionRequest(
      @NotBlank @Size(max = 2048) String endpoint, @NotNull @Valid Keys keys) {}

  public record Keys(
      @NotBlank @Size(max = 512) String p256dh, @NotBlank @Size(max = 512) String auth) {}

  public record DeleteRequest(@NotBlank @Size(max = 2048) String endpoint) {}
}
