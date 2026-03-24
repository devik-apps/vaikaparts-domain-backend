package com.devikapps.vaikaparts.client.pecunia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.pecunia.client.api.AirtelMoneyApi;
import com.devikapps.vaikaparts.pecunia.client.model.AirtelMoneyPayment;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentCurrency;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentProvider;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentType;
import com.devikapps.vaikaparts.pecunia.client.model.VerificationStatus;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

public class AirtelMoneyGetPaymentApiTest extends AbstractPecuniaTestBase {

  private AirtelMoneyApi subject;

  @BeforeEach
  void setUp() {
    subject = new AirtelMoneyApi(apiClient);
  }

  // -------------------------------------------------------------------------
  // Response deserialization
  // -------------------------------------------------------------------------

  @Test
  void should_return_payment_id_on_successful_retrieval() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response);
    assertEquals(AIRTEL_PAYMENT_ID, response.getId());
  }

  @Test
  void should_return_transaction_id_on_successful_retrieval() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response);
    assertEquals(AIRTEL_TRANSACTION_ID, response.getTransactionId());
  }

  @Test
  void should_return_success_status_on_completed_payment() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response);
    assertEquals(VerificationStatus.SUCCESS, response.getStatus());
  }

  @Test
  void should_return_pending_status_on_pending_payment() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.PENDING)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response);
    assertEquals(VerificationStatus.PENDING, response.getStatus());
  }

  @Test
  void should_return_failed_status_on_failed_payment() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.FAILED)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response);
    assertEquals(VerificationStatus.FAILED, response.getStatus());
  }

  @Test
  void should_return_airtel_money_provider() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response);
    assertEquals(PaymentProvider.AIRTEL_MONEY, response.getProvider());
  }

  @Test
  void should_return_correct_amount() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response);
    assertEquals(AMOUNT, response.getAmount());
  }

  @Test
  void should_return_correct_currency() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response);
    assertEquals(PaymentCurrency.AR, response.getCurrency());
  }

  @Test
  void should_return_correct_payment_type() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response);
    assertEquals(PaymentType.PROFILE_UNLOCK, response.getType());
  }

  @Test
  void should_return_correct_description() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response);
    assertEquals(DESCRIPTION, response.getDescription());
  }

  @Test
  void should_return_payer_phone_number() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response.getPayer());
    assertEquals(CUSTOMER_MSISDN, response.getPayer().getPhoneNumber());
  }

  @Test
  void should_return_airtel_money_id() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response);
    assertEquals(AIRTEL_MONEY_ID_UUID, response.getAirtelMoneyId());
  }

  @Test
  void should_return_null_airtel_money_id_when_absent() {
    mockWebServer.enqueue(jsonResponse(200, buildGetBodyWithoutAirtelMoneyId()));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response);
    assertNull(response.getAirtelMoneyId());
  }

  @Test
  void should_return_non_null_created_at() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response.getCreatedAt());
  }

  @Test
  void should_return_non_null_updated_at() {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    final AirtelMoneyPayment response = subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    assertNotNull(response.getUpdatedAt());
  }

  // -------------------------------------------------------------------------
  // HTTP request shape
  // -------------------------------------------------------------------------

  @Test
  void should_send_get_request_to_correct_path() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals("GET", recorded.getMethod());
    assertEquals("/v1/payments/airtel-money/" + AIRTEL_PAYMENT_ID, recorded.getPath());
  }

  @Test
  void should_include_transaction_id_in_path() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertNotNull(recorded.getPath());
    assertTrue(
        recorded.getPath().endsWith("/" + AIRTEL_PAYMENT_ID),
        "Path must end with the transaction UUID, got: " + recorded.getPath());
  }

  @Test
  void should_send_api_key_header() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildGetSuccessBody(VerificationStatus.SUCCESS)));

    subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(API_KEY, recorded.getHeader("X-API-KEY"));
  }

  // -------------------------------------------------------------------------
  // Error codes
  // -------------------------------------------------------------------------

  @Test
  void should_throw_on_400_bad_request() {
    mockWebServer.enqueue(jsonResponse(400, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID));
  }

  @Test
  void should_throw_on_401_unauthorized() {
    mockWebServer.enqueue(jsonResponse(401, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID));
  }

  @Test
  void should_throw_on_403_forbidden() {
    mockWebServer.enqueue(jsonResponse(403, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID));
  }

  @Test
  void should_throw_on_404_not_found() {
    mockWebServer.enqueue(jsonResponse(404, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID));
  }

  @Test
  void should_throw_on_500_internal_server_error() {
    mockWebServer.enqueue(jsonResponse(500, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID));
  }

  @Test
  void should_throw_on_502_bad_gateway() {
    mockWebServer.enqueue(jsonResponse(502, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID));
  }

  @Test
  void should_throw_on_503_service_unavailable() {
    mockWebServer.enqueue(jsonResponse(503, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID));
  }

  @Test
  void should_throw_on_504_gateway_timeout() {
    mockWebServer.enqueue(jsonResponse(504, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.getAirtelMoneyPayment(AIRTEL_PAYMENT_ID));
  }

  // -------------------------------------------------------------------------
  // Null-parameter guards
  // -------------------------------------------------------------------------

  @Test
  void should_throw_when_transaction_id_is_null() {
    assertThrows(RestClientResponseException.class, () -> subject.getAirtelMoneyPayment(null));
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private String buildGetSuccessBody(final VerificationStatus status) {
    return """
    {
        "id": "%s",
        "transaction_id": "%s",
        "description": "%s",
        "payer": {
            "phone_number": "%s"
        },
        "payee": {
            "phone_number": "%s"
        },
        "provider": "AIRTEL_MONEY",
        "type": "PROFILE_UNLOCK",
        "amount": %s,
        "currency": "AR",
        "status": "%s",
        "created_at": "2024-01-15T10:00:00",
        "updated_at": "2024-01-15T10:05:00",
        "airtel_money_id": "%s"
    }
    """
        .formatted(
            AIRTEL_PAYMENT_ID,
            AIRTEL_TRANSACTION_ID,
            DESCRIPTION,
            CUSTOMER_MSISDN,
            PAYEE_MSISDN,
            AMOUNT,
            status.getValue(),
            AIRTEL_MONEY_ID_UUID);
  }

  private String buildGetBodyWithoutAirtelMoneyId() {
    return """
    {
        "id": "%s",
        "transaction_id": "%s",
        "description": "%s",
        "payer": {
            "phone_number": "%s"
        },
        "provider": "AIRTEL_MONEY",
        "type": "PROFILE_UNLOCK",
        "amount": %s,
        "currency": "AR",
        "status": "PENDING",
        "created_at": "2024-01-15T10:00:00",
        "updated_at": "2024-01-15T10:00:00"
    }
    """
        .formatted(AIRTEL_PAYMENT_ID, AIRTEL_TRANSACTION_ID, DESCRIPTION, CUSTOMER_MSISDN, AMOUNT);
  }
}
