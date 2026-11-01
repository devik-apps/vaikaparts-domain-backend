package com.devikapps.vaikaparts.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devikapps.vaikaparts.config.JacksonConf;
import com.devikapps.vaikaparts.config.PecuniaConf;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PecuniaClientTest {

  private HttpServer server;
  private PecuniaClient client;

  @AfterEach
  void tearDown() {
    if (client != null) client.close();
    if (server != null) server.stop(0);
  }

  @Test
  void shouldInitiateDirectPapiPaymentWithStableReferenceAndOrangeCode() throws Exception {
    var requestBody = new AtomicReference<String>();
    var apiKey = new AtomicReference<String>();
    var path = new AtomicReference<String>();
    var method = new AtomicReference<String>();
    startServer(
        exchange -> {
          path.set(exchange.getRequestURI().getPath());
          method.set(exchange.getRequestMethod());
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          apiKey.set(exchange.getRequestHeaders().getFirst("X-Api-Key"));
          var body =
              """
              {"id":"payment-id","transaction_id":"unlock-id","merchant_reference":"unlock-id","status":"PENDING","amount":5000,"currency":"MGA","type":"PROFILE_UNLOCK","provider":"PAPI","payment_method":"ORANGE_MONEY","payer":{"name":"Ada","phone_number":"0340000000"}}
              """;
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(201, body.getBytes(StandardCharsets.UTF_8).length);
          exchange.getResponseBody().write(body.getBytes(StandardCharsets.UTF_8));
          exchange.close();
        });

    var response =
        client.initiatePapi(
            "unlock-id", new BigDecimal("5000"), "ORANGE_MONEY", "Ada", "0340000000", "123456");

    assertEquals("/v1/payments/papi", path.get());
    assertEquals("POST", method.get());
    assertEquals("secret", apiKey.get());
    var json = new JacksonConf().objectMapper().readTree(requestBody.get());
    assertEquals(5000, json.get("amount").asInt());
    assertEquals("unlock-id", json.get("reference").asText());
    assertEquals("PROFILE_UNLOCK", json.get("type").asText());
    assertEquals("ORANGE_MONEY", json.get("payment_method").asText());
    assertEquals("Ada", json.get("payer").get("name").asText());
    assertEquals("0340000000", json.get("payer").get("phone_number").asText());
    assertEquals("123456", json.get("temporary_code").asText());
    assertEquals("payment-id", response.id());
    assertEquals("PENDING", response.status());
  }

  @Test
  void shouldLookupPapiPaymentByMerchantReference() throws Exception {
    var path = new AtomicReference<String>();
    var method = new AtomicReference<String>();
    startServer(
        exchange -> {
          path.set(exchange.getRequestURI().getPath());
          method.set(exchange.getRequestMethod());
          var body =
              """
              {"id":"payment-id","transaction_id":"unlock-id","merchant_reference":"unlock-id","status":"SUCCESS","amount":5000,"currency":"MGA","type":"PROFILE_UNLOCK","provider":"PAPI","payment_method":"MVOLA","payer":{"name":"Ada","phone_number":"0340000000"}}
              """;
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.getBytes(StandardCharsets.UTF_8).length);
          exchange.getResponseBody().write(body.getBytes(StandardCharsets.UTF_8));
          exchange.close();
        });

    var response = client.findPapiByReference("unlock-id");

    assertEquals("/v1/payments/papi/unlock-id", path.get());
    assertEquals("GET", method.get());
    assertEquals("SUCCESS", response.status());
    assertEquals("unlock-id", response.merchantReference());
  }

  @Test
  void shouldRejectNonSuccessfulPapiResponse() throws Exception {
    startServer(
        exchange -> {
          exchange.sendResponseHeaders(503, -1);
          exchange.close();
        });

    assertThrows(PecuniaClientException.class, () -> client.findPapiByReference("unlock-id"));
  }

  @Test
  void shouldIdentifyMissingPapiPaymentForSafeSameReferenceRetry() throws Exception {
    startServer(
        exchange -> {
          exchange.sendResponseHeaders(404, -1);
          exchange.close();
        });

    assertThrows(PecuniaNotFoundException.class, () -> client.findPapiByReference("unlock-id"));
  }

  private void startServer(com.sun.net.httpserver.HttpHandler handler) throws Exception {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/", handler);
    server.start();
    var conf = new PecuniaConf();
    conf.setBaseUrl("http://localhost:" + server.getAddress().getPort());
    conf.setApiKey("secret");
    conf.setConnectTimeout(Duration.ofSeconds(2));
    conf.setRequestTimeout(Duration.ofSeconds(2));
    client = new PecuniaClient(conf, new JacksonConf().objectMapper());
  }
}
