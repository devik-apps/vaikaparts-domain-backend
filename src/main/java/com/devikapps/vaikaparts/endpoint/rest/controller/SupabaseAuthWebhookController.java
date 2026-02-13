package com.devikapps.vaikaparts.endpoint.rest.controller;

import com.devikapps.vaikaparts.service.SupabaseAuthWebhookService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/spb")
@RequiredArgsConstructor
public class SupabaseAuthWebhookController {

  private static final String SIGNATURE_HEADER = "X-Webhook-Signature";

  private final SupabaseAuthWebhookService supabaseAuthWebhookService;

  @PostMapping("/auth")
  public ResponseEntity<Map<String, String>> handleAuthWebhook(
      @RequestBody String rawPayload, @RequestHeader(value = SIGNATURE_HEADER) String signature) {

    Map<String, String> response =
        supabaseAuthWebhookService.handleAuthWebhook(rawPayload, signature);
    return ResponseEntity.ok(response);
  }
}
