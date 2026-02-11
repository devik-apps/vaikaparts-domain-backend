package com.devikapps.vaikaparts.validator;

import static com.devikapps.vaikaparts.conf.EnvConf.SUPABASE_WEBHOOK_SECRET;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.WebhookSignaturePayload;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WebhookSignatureValidatorIT extends FacadeIT {

  private static final String TEST_PAYLOAD = "{\"userId\":\"123\",\"email\":\"test@example.com\"}";
  private static final String EMPTY_STRING = "";
  private static final String BLANK_STRING = "   ";

  @Autowired private WebhookSignatureValidator webhookSignatureValidator;

  @Test
  void validate_should_pass_when_signature_is_valid() throws Exception {
    String validSignature = generateValidSignature(TEST_PAYLOAD);
    WebhookSignaturePayload payload = new WebhookSignaturePayload(TEST_PAYLOAD, validSignature);

    assertDoesNotThrow(() -> webhookSignatureValidator.validate(payload));
  }

  @Test
  void validate_should_throw_security_exception_when_signature_is_invalid() {
    String invalidSignature = "invalid-signature";
    WebhookSignaturePayload payload = new WebhookSignaturePayload(TEST_PAYLOAD, invalidSignature);

    SecurityException exception =
        assertThrows(SecurityException.class, () -> webhookSignatureValidator.validate(payload));
    assertEquals("Invalid webhook signature", exception.getMessage());
  }

  @Test
  void validate_should_throw_security_exception_when_signature_is_null() {
    WebhookSignaturePayload payload = new WebhookSignaturePayload(TEST_PAYLOAD, null);

    SecurityException exception =
        assertThrows(SecurityException.class, () -> webhookSignatureValidator.validate(payload));
    assertEquals("Webhook signature cannot be null or empty", exception.getMessage());
  }

  @Test
  void validate_should_throw_security_exception_when_signature_is_empty() {
    WebhookSignaturePayload payload = new WebhookSignaturePayload(TEST_PAYLOAD, EMPTY_STRING);

    SecurityException exception =
        assertThrows(SecurityException.class, () -> webhookSignatureValidator.validate(payload));
    assertEquals("Webhook signature cannot be null or empty", exception.getMessage());
  }

  @Test
  void validate_should_throw_security_exception_when_signature_is_blank() {
    WebhookSignaturePayload payload = new WebhookSignaturePayload(TEST_PAYLOAD, BLANK_STRING);

    SecurityException exception =
        assertThrows(SecurityException.class, () -> webhookSignatureValidator.validate(payload));
    assertEquals("Webhook signature cannot be null or empty", exception.getMessage());
  }

  @Test
  void validate_should_throw_illegal_argument_exception_when_payload_is_null() throws Exception {
    String validSignature = generateValidSignature(TEST_PAYLOAD);
    WebhookSignaturePayload payload = new WebhookSignaturePayload(null, validSignature);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> webhookSignatureValidator.validate(payload));
    assertEquals("Webhook payload cannot be null or empty", exception.getMessage());
  }

  @Test
  void validate_should_throw_illegal_argument_exception_when_payload_is_empty() throws Exception {
    String validSignature = generateValidSignature(EMPTY_STRING);
    WebhookSignaturePayload payload = new WebhookSignaturePayload(EMPTY_STRING, validSignature);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> webhookSignatureValidator.validate(payload));
    assertEquals("Webhook payload cannot be null or empty", exception.getMessage());
  }

  @Test
  void validate_should_throw_illegal_argument_exception_when_payload_is_blank() throws Exception {
    String validSignature = generateValidSignature(BLANK_STRING);
    WebhookSignaturePayload payload = new WebhookSignaturePayload(BLANK_STRING, validSignature);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> webhookSignatureValidator.validate(payload));
    assertEquals("Webhook payload cannot be null or empty", exception.getMessage());
  }

  @Test
  void validate_should_throw_security_exception_when_signature_mismatch_with_different_payload()
      throws Exception {
    String differentPayload = "{\"userId\":\"456\",\"email\":\"other@example.com\"}";
    String signatureForOriginalPayload = generateValidSignature(TEST_PAYLOAD);
    WebhookSignaturePayload payload =
        new WebhookSignaturePayload(differentPayload, signatureForOriginalPayload);

    SecurityException exception =
        assertThrows(SecurityException.class, () -> webhookSignatureValidator.validate(payload));
    assertEquals("Invalid webhook signature", exception.getMessage());
  }

  @Test
  void validate_should_pass_when_payload_contains_special_characters() throws Exception {
    String specialPayload =
        "{\"userId\":\"123\",\"name\":\"Test\\n"
            + "User\",\"description\":\"A test with \\\"quotes\\\"\"}";
    String validSignature = generateValidSignature(specialPayload);
    WebhookSignaturePayload payload = new WebhookSignaturePayload(specialPayload, validSignature);

    assertDoesNotThrow(() -> webhookSignatureValidator.validate(payload));
  }

  @Test
  void validate_should_pass_when_payload_is_large() throws Exception {
    String largePayload = "x".repeat(10000);
    String validSignature = generateValidSignature(largePayload);
    WebhookSignaturePayload payload = new WebhookSignaturePayload(largePayload, validSignature);

    assertDoesNotThrow(() -> webhookSignatureValidator.validate(payload));
  }

  @Test
  void get_validated_type_should_return_webhook_signature_payload_class() {
    Class<WebhookSignaturePayload> validatedType = webhookSignatureValidator.getValidatedType();

    assertNotNull(validatedType);
    assertEquals(WebhookSignaturePayload.class, validatedType);
  }

  private String generateValidSignature(String payload) throws Exception {
    Mac hmacSha256 = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey =
        new SecretKeySpec(SUPABASE_WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    hmacSha256.init(secretKey);
    byte[] hash = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(hash);
  }
}
