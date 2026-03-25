package com.devikapps.vaikaparts.client.pecunia;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.pecunia.client.api.MVolaApi;
import com.devikapps.vaikaparts.pecunia.client.model.MvolaNotificationMethod;
import com.devikapps.vaikaparts.pecunia.client.model.MvolaPayment;
import com.devikapps.vaikaparts.pecunia.client.model.MvolaPaymentRequest;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentCurrency;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentParty;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentProvider;
import com.devikapps.vaikaparts.pecunia.client.model.PaymentType;
import com.devikapps.vaikaparts.pecunia.client.model.VerificationStatus;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

public class MvolaInitiatePaymentApiTest extends AbstractPecuniaTestBase {

  private MVolaApi subject;

  @BeforeEach
  void setUp() {
    subject = new MVolaApi(apiClient);
  }

  @Test
  void should_return_payment_id_on_successful_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(MVOLA_PAYMENT_ID, response.getId());
  }

  @Test
  void should_return_transaction_id_on_successful_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(MVOLA_TRANSACTION_ID, response.getTransactionId());
  }

  @Test
  void should_return_server_correlation_id_on_successful_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(MVOLA_SERVER_CORRELATION_ID, response.getServerCorrelationId());
  }

  @Test
  void should_return_pending_status_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(VerificationStatus.PENDING, response.getStatus());
  }

  @Test
  void should_return_mvola_provider_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(PaymentProvider.MVOLA, response.getProvider());
  }

  @Test
  void should_return_correct_amount_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(AMOUNT, response.getAmount());
  }

  @Test
  void should_return_correct_currency_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(PaymentCurrency.AR, response.getCurrency());
  }

  @Test
  void should_return_correct_payment_type_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(PaymentType.PROFILE_UNLOCK, response.getType());
  }

  @Test
  void should_return_correct_description_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(DESCRIPTION, response.getDescription());
  }

  @Test
  void should_return_payer_msisdn_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response.getPayer());
    assertEquals(CUSTOMER_MSISDN, response.getPayer().getPhoneNumber());
  }

  @Test
  void should_return_callback_notification_method_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(MvolaNotificationMethod.CALLBACK, response.getNotificationMethod());
  }

  @Test
  void should_return_polling_notification_method_when_specified() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateBodyWithPollingMethod()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response);
    assertEquals(MvolaNotificationMethod.POLLING, response.getNotificationMethod());
  }

  @Test
  void should_return_null_mvola_transaction_id_when_absent() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response);
    assertNull(response.getMvolaTransactionId());
  }

  @Test
  void should_return_non_null_created_at_on_initiation() {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    final MvolaPayment response = subject.initiateMvolaPayment(buildValidRequest());

    assertNotNull(response.getCreatedAt());
  }

  @Test
  void should_send_post_request_to_correct_path() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateMvolaPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals("POST", recorded.getMethod());
    assertEquals("/v1/payments/mvola", recorded.getPath());
  }

  @Test
  void should_send_api_key_header() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateMvolaPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(API_KEY, recorded.getHeader("X-API-KEY"));
  }

  @Test
  void should_send_content_type_json_header() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateMvolaPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertNotNull(recorded.getHeader("Content-Type"));
    assertTrue(requireNonNull(recorded.getHeader("Content-Type")).contains("application/json"));
  }

  @Test
  void should_send_payer_msisdn_in_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateMvolaPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains(CUSTOMER_MSISDN));
  }

  @Test
  void should_send_amount_in_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateMvolaPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains("5000"));
  }

  @Test
  void should_send_description_in_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateMvolaPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains(DESCRIPTION));
  }

  @Test
  void should_send_payment_type_in_body() throws Exception {
    mockWebServer.enqueue(jsonResponse(201, buildInitiateSuccessBody()));

    subject.initiateMvolaPayment(buildValidRequest());

    final RecordedRequest recorded = mockWebServer.takeRequest();
    final String body = recorded.getBody().readUtf8();
    assertTrue(body.contains("PROFILE_UNLOCK"));
  }

  @Test
  void should_throw_on_400_bad_request() {
    mockWebServer.enqueue(jsonResponse(400, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.initiateMvolaPayment(buildValidRequest()));
  }

  @Test
  void should_throw_on_401_unauthorized() {
    mockWebServer.enqueue(jsonResponse(401, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.initiateMvolaPayment(buildValidRequest()));
  }

  @Test
  void should_throw_on_403_forbidden() {
    mockWebServer.enqueue(jsonResponse(403, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.initiateMvolaPayment(buildValidRequest()));
  }

  @Test
  void should_throw_on_500_internal_server_error() {
    mockWebServer.enqueue(jsonResponse(500, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.initiateMvolaPayment(buildValidRequest()));
  }

  @Test
  void should_throw_on_502_bad_gateway() {
    mockWebServer.enqueue(jsonResponse(502, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.initiateMvolaPayment(buildValidRequest()));
  }

  @Test
  void should_throw_on_503_service_unavailable() {
    mockWebServer.enqueue(jsonResponse(503, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.initiateMvolaPayment(buildValidRequest()));
  }

  @Test
  void should_throw_on_504_gateway_timeout() {
    mockWebServer.enqueue(jsonResponse(504, "{}"));

    assertThrows(
        RestClientResponseException.class, () -> subject.initiateMvolaPayment(buildValidRequest()));
  }

  @Test
  void should_throw_when_request_is_null() {
    assertThrows(RestClientResponseException.class, () -> subject.initiateMvolaPayment(null));
  }

  private MvolaPaymentRequest buildValidRequest() {
    final PaymentParty payer = new PaymentParty();
    payer.setPhoneNumber(CUSTOMER_MSISDN);

    final MvolaPaymentRequest request = new MvolaPaymentRequest();
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
        "provider": "MVOLA",
        "type": "PROFILE_UNLOCK",
        "amount": %s,
        "currency": "AR",
        "status": "PENDING",
        "created_at": "2024-01-15T10:00:00",
        "updated_at": "2024-01-15T10:00:00",
        "server_correlation_id": "%s",
        "notification_method": "callback"
    }
    """
        .formatted(
            MVOLA_PAYMENT_ID,
            MVOLA_TRANSACTION_ID,
            DESCRIPTION,
            CUSTOMER_MSISDN,
            PAYEE_MSISDN,
            AMOUNT,
            MVOLA_SERVER_CORRELATION_ID);
  }

  private String buildInitiateBodyWithPollingMethod() {
    return """
    {
        "id": "%s",
        "transaction_id": "%s",
        "description": "%s",
        "payer": {
            "phone_number": "%s"
        },
        "provider": "MVOLA",
        "type": "PROFILE_UNLOCK",
        "amount": %s,
        "currency": "AR",
        "status": "PENDING",
        "created_at": "2024-01-15T10:00:00",
        "updated_at": "2024-01-15T10:00:00",
        "server_correlation_id": "%s",
        "notification_method": "polling"
    }
    """
        .formatted(
            MVOLA_PAYMENT_ID,
            MVOLA_TRANSACTION_ID,
            DESCRIPTION,
            CUSTOMER_MSISDN,
            AMOUNT,
            MVOLA_SERVER_CORRELATION_ID);
  }
}
