package com.devikapps.vaikaparts.validator;

import com.devikapps.vaikaparts.config.SupabaseConf;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.WebhookSignaturePayload;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Component
@Validated
@RequiredArgsConstructor
public class WebhookSignatureValidator implements Validator<WebhookSignaturePayload> {

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private final SupabaseConf supabaseConf;

  @Override
  public void validate(@NotNull WebhookSignaturePayload input) {
    log.debug("Validating webhook signature");

    if (input.payload() == null || input.payload().isBlank()) {
      log.error("Webhook signature validation failed: payload is null or empty");
      throw new IllegalArgumentException("Webhook payload cannot be null or empty");
    }

    if (input.providedSignature() == null || input.providedSignature().isBlank()) {
      log.error("Webhook signature validation failed: signature is null or empty");
      throw new SecurityException("Webhook signature cannot be null or empty");
    }

    try {
      String computedSignature = computeSignature(input.payload());
      boolean isValid =
          MessageDigest.isEqual(
              computedSignature.getBytes(StandardCharsets.UTF_8),
              input.providedSignature().getBytes(StandardCharsets.UTF_8));

      if (!isValid) {
        log.warn("Webhook signature validation failed: signature mismatch");
        throw new SecurityException("Invalid webhook signature");
      }

      log.debug("Webhook signature validated successfully");
    } catch (NoSuchAlgorithmException e) {
      log.error(
          "Webhook signature validation failed: algorithm not available - {}", e.getMessage());
      throw new IllegalStateException("HMAC algorithm not available", e);
    } catch (InvalidKeyException e) {
      log.error("Webhook signature validation failed: invalid key - {}", e.getMessage());
      throw new IllegalStateException("Invalid webhook secret key", e);
    }
  }

  @Override
  public Class<WebhookSignaturePayload> getValidatedType() {
    return WebhookSignaturePayload.class;
  }

  private String computeSignature(String payload)
      throws NoSuchAlgorithmException, InvalidKeyException {
    Mac hmacSha256 = Mac.getInstance(HMAC_ALGORITHM);
    SecretKeySpec secretKey =
        new SecretKeySpec(
            supabaseConf.getWebhookSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    hmacSha256.init(secretKey);
    byte[] hash = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(hash);
  }
}
