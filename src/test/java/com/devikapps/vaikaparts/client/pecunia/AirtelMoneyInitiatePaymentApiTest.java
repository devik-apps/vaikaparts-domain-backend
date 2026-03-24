package com.devikapps.vaikaparts.client.pecunia;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.pecunia.client.api.AirtelMoneyApi;
import com.devikapps.vaikaparts.pecunia.client.model.AirtelMoneyPayment;
import com.devikapps.vaikaparts.pecunia.client.model.AirtelMoneyPaymentRequest;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentCurrency;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentParty;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentProvider;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentType;
import com.devikapps.vaikaparts.pecunia.client.model.VerificationStatus;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

public class AirtelMoneyInitiatePaymentApiTest extends AbstractPecuniaTestBase {

  private AirtelMoneyApi subject;

  @BeforeEach
  void setUp() {
    subject = new AirtelMoneyApi(apiClient);
  }

  @Test
  void should_return_payment_id_on_successful_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final AirtelMoneyPayment response = subject.initiateAirtelMoneyPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(AIRTEL_PAYMENT_ID, response.getId());
  }

  @Test
  void should_return_transaction_id_on_successful_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final AirtelMoneyPayment response = subject.initiateAirtelMoneyPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(AIRTEL_TRANSACTION_ID, response.getTransactionId());
  }

  @Test
  void should_return_pending_status_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final AirtelMoneyPayment response = subject.initiateAirtelMoneyPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(VerificationStatus.PENDING, response.getStatus());
  }

  @Test
  void should_return_airtel_money_provider_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final AirtelMoneyPayment response = subject.initiateAirtelMoneyPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(PaymentProvider.AIRTEL_MONEY, response.getProvider());
  }

  @Test
  void should_return_correct_amount_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final AirtelMoneyPayment response = subject.initiateAirtelMoneyPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(AMOUNT, response.getAmount());
  }

  @Test
  void should_return_correct_currency_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final AirtelMoneyPayment response = subject.initiateAirtelMoneyPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(PaymentCurrency.AR, response.getCurrency());
  }

  @Test
  void should_return_correct_payment_type_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final AirtelMoneyPayment response = subject.initiateAirtelMoneyPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(PaymentType.PROFILE_UNLOCK, response.getType());
  }

  @Test
  void should_return_correct_description_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final AirtelMoneyPayment response = subject.initiateAirtelMoneyPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(DESCRIPTION, response.getDescription());
  }

  @Test
  void should_return_payer_msisdn_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final AirtelMoneyPayment response = subject.initiateAirtelMoneyPayment(buildValidRequest());

    assertNotNull(response.getPayer());
    assertEquals(CUSTOMER_MSISDN, response.getPayer().getPhoneNumber());
  }

  @Test
  void should_return_airtel_money_id_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final AirtelMoneyPayment response = subject.initiateAirtelMoneyPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(AIRTEL_MONEY_ID_UUID, response.getAirtelMoneyId());
  }

  @Test
  void should_return_null_airtel_money_id_when_absent_in_body() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateBodyWithoutAirtelMoneyId()));

    final AirtelMoneyPayment response = subject.initiateAirtelMoneyPayment(buildValidRequest());

    assertNotNull(response);
    assertNull(response.getAirtelMoneyId());
  }

  @Test
  void should_return_non_null_created_at_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final AirtelMoneyPayment response = subject.initiateAirtelMoneyPayment(buildValidRequest());

    assertNotNull(response.getCreatedAt());
  }

  // -------------------------------------------------------------------------
  // HTTP request shape
  // -------------------------------------------------------------------------

  @Test
  void should_send_post_request_to_correct_path() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateAirtelMoneyPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals("POST", recorded.getMethod());
    assertEquals("/v1/payments/airtel-money", recorded.getPath());
  }

  @Test
  void should_send_api_key_header() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateAirtelMoneyPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(API_KEY, recorded.getHeader("X-API-KEY"));
  }

  @Test
  void should_send_content_type_json_header() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateAirtelMoneyPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertNotNull(recorded.getHeader("Content-Type"));
    assertTrue(requireNonNull(recorded.getHeader("Content-Type")).contains("application/json"));
  }

  @Test
  void should_send_payer_msisdn_in_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateAirtelMoneyPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains(CUSTOMER_MSISDN));
  }

  @Test
  void should_send_amount_in_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateAirtelMoneyPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains("5000"));
  }

  @Test
  void should_send_description_in_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateAirtelMoneyPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains(DESCRIPTION));
  }

  @Test
  void should_send_payment_type_in_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateAirtelMoneyPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains("PROFILE_UNLOCK"));
  }

  // -------------------------------------------------------------------------
  // Error codes
  // -------------------------------------------------------------------------

  @Test
  void should_throw_on_400_bad_request() {
    mockWebServer.enqueue(jsonResponse(400, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.initiateAirtelMoneyPayment(buildValidRequest()));
  }

  @Test
  void should_throw_on_401_unauthorized() {
    mockWebServer.enqueue(jsonResponse(401, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.initiateAirtelMoneyPayment(buildValidRequest()));
  }

  @Test
  void should_throw_on_403_forbidden() {
    mockWebServer.enqueue(jsonResponse(403, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.initiateAirtelMoneyPayment(buildValidRequest()));
  }

  @Test
  void should_throw_on_500_internal_server_error() {
    mockWebServer.enqueue(jsonResponse(500, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.initiateAirtelMoneyPayment(buildValidRequest()));
  }

  @Test
  void should_throw_on_502_bad_gateway() {
    mockWebServer.enqueue(jsonResponse(502, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.initiateAirtelMoneyPayment(buildValidRequest()));
  }

  @Test
  void should_throw_on_503_service_unavailable() {
    mockWebServer.enqueue(jsonResponse(503, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.initiateAirtelMoneyPayment(buildValidRequest()));
  }

  @Test
  void should_throw_on_504_gateway_timeout() {
    mockWebServer.enqueue(jsonResponse(504, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.initiateAirtelMoneyPayment(buildValidRequest()));
  }

  // -------------------------------------------------------------------------
  // Null-parameter guards
  // -------------------------------------------------------------------------

  @Test
  void should_throw_when_request_is_null() {
    assertThrows(RestClientResponseException.class, () -> subject.initiateAirtelMoneyPayment(null));
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private AirtelMoneyPaymentRequest buildValidRequest() {
    final PaymentParty payer = new PaymentParty();
    payer.setPhoneNumber(CUSTOMER_MSISDN);

    final AirtelMoneyPaymentRequest request = new AirtelMoneyPaymentRequest();
    request.setAmount(AMOUNT);
    request.setCurrency(PaymentCurrency.AR);
    request.setDescription(DESCRIPTION);
    request.setPayer(payer);
    request.setType(PaymentType.PROFILE_UNLOCK);
    return request;
  }

  private String buildInitiateSuccessBody() {
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
        "status": "PENDING",
        "created_at": "2024-01-15T10:00:00",
        "updated_at": "2024-01-15T10:00:00",
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
            AIRTEL_MONEY_ID_UUID);
  }

  private String buildInitiateBodyWithoutAirtelMoneyId() {
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
