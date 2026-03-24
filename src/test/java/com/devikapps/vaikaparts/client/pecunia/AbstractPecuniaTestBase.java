package com.devikapps.vaikaparts.client.pecunia;

import static java.util.UUID.randomUUID;

import com.devikapps.vaikaparts.pecunia.client.invoker.ApiClient;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractPecuniaTestBase {

  public static final UUID MVOLA_PAYMENT_ID = randomUUID();
  public static final UUID AIRTEL_PAYMENT_ID = randomUUID();
  public static final UUID MVOLA_SERVER_CORRELATION_ID = randomUUID();
  public static final UUID AIRTEL_MONEY_ID_UUID = randomUUID();
  public static final String MVOLA_TRANSACTION_ID = "mvola-txn-ref-001";
  public static final String AIRTEL_TRANSACTION_ID = "airtel-txn-ref-001";
  public static final String CUSTOMER_MSISDN = "+261330000001";
  public static final String CUSTOMER_MSISDN_PLAIN = "330000001";
  public static final String PAYEE_MSISDN = "+261340000002";
  public static final BigDecimal AMOUNT = new BigDecimal("5000.00");
  public static final String DESCRIPTION = "Profile unlock payment";
  public static final String API_KEY = randomUUID().toString();

  protected MockWebServer mockWebServer;
  protected ApiClient apiClient;

  @BeforeEach
  void setUpServer() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    apiClient = new ApiClient();
    apiClient.setBasePath(mockWebServer.url("/").toString().replaceAll("/$", ""));
    apiClient.setApiKey(API_KEY);
  }

  @AfterEach
  void tearDownServer() throws IOException {
    mockWebServer.shutdown();
  }

  public MockResponse jsonResponse(final int code, final String body) {
    return new MockResponse()
        .setResponseCode(code)
        .setBody(body)
        .addHeader("Content-Type", "application/json");
  }
}
