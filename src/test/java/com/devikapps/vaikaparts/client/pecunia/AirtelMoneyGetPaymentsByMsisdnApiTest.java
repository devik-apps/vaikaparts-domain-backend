package com.devikapps.vaikaparts.client.pecunia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.pecunia.client.api.AirtelMoneyApi;
import com.devikapps.vaikaparts.pecunia.client.model.AirtelMoneyPaymentPageResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

public class AirtelMoneyGetPaymentsByMsisdnApiTest extends AbstractPecuniaTestBase {

  private AirtelMoneyApi subject;

  @BeforeEach
  void setUp() {
    subject = new AirtelMoneyApi(apiClient);
  }

  @Test
  void should_return_non_empty_content_on_successful_retrieval() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertFalse(response.getContent().isEmpty());
  }

  @Test
  void should_return_correct_total_elements() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertEquals(1L, response.getTotalElements());
  }

  @Test
  void should_return_correct_total_pages() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertEquals(1, response.getTotalPages());
  }

  @Test
  void should_return_first_true_on_first_page() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertTrue(response.getFirst());
  }

  @Test
  void should_return_last_true_on_single_page_result() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, true, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertTrue(response.getLast());
  }

  @Test
  void should_return_last_false_when_more_pages_exist() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(3, 20, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertFalse(response.getLast());
  }

  @Test
  void should_return_empty_false_when_content_present() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertFalse(response.getEmpty());
  }

  @Test
  void should_return_empty_true_on_empty_result() {
    mockWebServer.enqueue(jsonResponse(200, buildEmptyPageResponseBody()));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertTrue(response.getEmpty());
    assertTrue(response.getContent().isEmpty());
    assertEquals(0L, response.getTotalElements());
  }

  @Test
  void should_return_correct_page_number() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertEquals(0, response.getNumber());
  }

  @Test
  void should_return_correct_page_size() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertEquals(10, response.getSize());
  }

  @Test
  void should_return_correct_number_of_elements() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertEquals(1, response.getNumberOfElements());
  }

  @Test
  void should_return_correct_payment_id_in_content() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertFalse(response.getContent().isEmpty());
    assertEquals(AIRTEL_PAYMENT_ID, response.getContent().get(0).getId());
  }

  @Test
  void should_return_correct_status_in_content() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response);
    assertFalse(response.getContent().isEmpty());
    assertEquals("SUCCESS", response.getContent().get(0).getStatus().getValue());
  }

  @Test
  void should_return_pageable_with_correct_page_number() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response.getPageable());
    assertEquals(0, response.getPageable().getPageNumber());
  }

  @Test
  void should_return_pageable_with_correct_page_size() {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    final AirtelMoneyPaymentPageResponse response =
        subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    assertNotNull(response.getPageable());
    assertEquals(10, response.getPageable().getPageSize());
  }

  @Test
  void should_send_get_request_to_correct_path() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals("GET", recorded.getMethod());
    assertTrue(
        recorded.getPath().startsWith("/v1/payments/airtel-money/customer/"),
        "Path must start with the expected prefix, got: " + recorded.getPath());
  }

  @Test
  void should_include_customer_msisdn_in_path() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertNotNull(recorded.getPath());
    assertTrue(
        recorded.getPath().contains(CUSTOMER_MSISDN_PLAIN),
        "Path must contain the customer MSISDN, got: " + recorded.getPath());
  }

  @Test
  void should_include_page_and_size_query_params() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertNotNull(recorded.getPath());
    assertTrue(recorded.getPath().contains("page=0"), "Should contain page query param");
    assertTrue(recorded.getPath().contains("size=10"), "Should contain size query param");
  }

  @Test
  void should_send_api_key_header() throws Exception {
    mockWebServer.enqueue(jsonResponse(200, buildPageResponseBody(1, 1, false, true)));

    subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10);

    final RecordedRequest recorded = mockWebServer.takeRequest();
    assertEquals(API_KEY, recorded.getHeader("X-API-KEY"));
  }

  @Test
  void should_throw_on_400_bad_request() {
    mockWebServer.enqueue(jsonResponse(400, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10));
  }

  @Test
  void should_throw_on_401_unauthorized() {
    mockWebServer.enqueue(jsonResponse(401, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10));
  }

  @Test
  void should_throw_on_403_forbidden() {
    mockWebServer.enqueue(jsonResponse(403, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10));
  }

  @Test
  void should_throw_on_500_internal_server_error() {
    mockWebServer.enqueue(jsonResponse(500, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10));
  }

  @Test
  void should_throw_on_502_bad_gateway() {
    mockWebServer.enqueue(jsonResponse(502, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10));
  }

  @Test
  void should_throw_on_504_gateway_timeout() {
    mockWebServer.enqueue(jsonResponse(504, "{}"));

    assertThrows(
        RestClientResponseException.class,
        () -> subject.getAirtelMoneyPaymentsByCustomerMsisdn(CUSTOMER_MSISDN_PLAIN, 0, 10));
  }

  @Test
  void should_throw_when_customer_msisdn_is_null() {
    assertThrows(
        RestClientResponseException.class,
        () -> subject.getAirtelMoneyPaymentsByCustomerMsisdn(null, 0, 10));
  }

  private String buildPageResponseBody(
      final int totalPages, final long totalElements, final boolean last, final boolean first) {
    return """
    {
        "content": [
            {
                "id": "%s",
                "transaction_id": "%s",
                "description": "%s",
                "payer": { "phone_number": "%s" },
                "provider": "AIRTEL_MONEY",
                "type": "PROFILE_UNLOCK",
                "amount": %s,
                "currency": "AR",
                "status": "SUCCESS",
                "created_at": "2024-01-15T10:00:00",
                "updated_at": "2024-01-15T10:05:00",
                "airtel_money_id": "%s"
            }
        ],
        "pageable": {
            "page_number": 0,
            "page_size": 10,
            "offset": 0,
            "paged": true,
            "unpaged": false
        },
        "total_elements": %d,
        "total_pages": %d,
        "last": %b,
        "first": %b,
        "size": 10,
        "number": 0,
        "number_of_elements": 1,
        "empty": false
    }
    """
        .formatted(
            AIRTEL_PAYMENT_ID,
            AIRTEL_TRANSACTION_ID,
            DESCRIPTION,
            CUSTOMER_MSISDN,
            AMOUNT,
            AIRTEL_MONEY_ID_UUID,
            totalElements,
            totalPages,
            last,
            first);
  }

  private String buildEmptyPageResponseBody() {
    return """
    {
        "content": [],
        "pageable": {
            "page_number": 0,
            "page_size": 10,
            "offset": 0,
            "paged": true,
            "unpaged": false
        },
        "total_elements": 0,
        "total_pages": 0,
        "last": true,
        "first": true,
        "size": 10,
        "number": 0,
        "number_of_elements": 0,
        "empty": true
    }
    """;
  }
}
