package com.devikapps.vaikaparts.client;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devikapps.vaikaparts.client.api.WebhooksApi;
import com.devikapps.vaikaparts.client.invoker.ApiClient;
import com.devikapps.vaikaparts.client.model.SupabaseDatabaseWebhook;
import com.devikapps.vaikaparts.client.model.SupabaseDatabaseWebhook.TypeEnum;
import com.devikapps.vaikaparts.client.model.SupabaseProfileRecord;
import com.devikapps.vaikaparts.client.model.SupabaseProfileRecordUserMetadata;
import com.devikapps.vaikaparts.client.model.SupabaseProfileRecordUserMetadata.UserTypeEnum;
import com.devikapps.vaikaparts.client.model.SupabaseProfileWebhook200Response;
import com.devikapps.vaikaparts.client.model.SupabaseProfileWebhook200Response.EventTypeEnum;
import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.config.SupabaseConf;
import com.devikapps.vaikaparts.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class WebhookClientIT extends FacadeIT {

  private static final String BASE_URL = "http://localhost";
  private static final String WEBHOOK_ENDPOINT = "/v1/webhooks/spb/auth";
  private static final String SIGNATURE_HEADER = "X-Webhook-Signature";

  private static final String TEST_TABLE = "profiles";
  private static final String TEST_SCHEMA = "public";

  @LocalServerPort private int port;

  @Autowired private UserRepository userRepository;
  @Autowired private SupabaseConf spbConf;
  @Autowired private TestRestTemplate restTemplate;

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  void supabase_profile_webhook_test() {
    SupabaseDatabaseWebhook insertPayload = buildInsertPayload(UserTypeEnum.RESEARCHER);

    WebhooksApi signedClient = buildClient(spbConf.getWebhookSecret());
    ResponseEntity<SupabaseProfileWebhook200Response> ok =
        signedClient.supabaseProfileWebhookWithHttpInfo(insertPayload);

    assertThat(ok.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());

    SupabaseProfileWebhook200Response body = ok.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getMessage()).isNotBlank();
    assertThat(body.getUserId()).isNotNull();
    assertThat(body.getUserId().toString()).isEqualTo(insertPayload.getRecord().getId().toString());
    assertThat(body.getEventType()).isEqualTo(EventTypeEnum.INSERT);

    // 401 — missing signature header
    assertThatThrownBy(
            () -> buildClient(null).supabaseProfileWebhook(buildInsertPayload(UserTypeEnum.SELLER)))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));

    // 401 — invalid signature value
    assertThatThrownBy(
            () ->
                buildClient("invalid-signature")
                    .supabaseProfileWebhook(buildInsertPayload(UserTypeEnum.SELLER)))
        .isInstanceOf(RestClientResponseException.class)
        .satisfies(
            ex ->
                assertThat(((RestClientResponseException) ex).getStatusCode().value())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));

    // 400 — malformed JSON payload
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(SIGNATURE_HEADER, spbConf.getWebhookSecret());

    ResponseEntity<String> malformed =
        restTemplate.exchange(
            format("%s:%s%s", BASE_URL, port, WEBHOOK_ENDPOINT),
            HttpMethod.POST,
            new HttpEntity<>("{invalid json", headers),
            String.class);

    assertThat(malformed.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  private WebhooksApi buildClient(String webhookSecret) {
    RestClient.Builder builder = RestClient.builder().baseUrl(format("%s:%s", BASE_URL, port));

    if (webhookSecret != null) builder.defaultHeader(SIGNATURE_HEADER, webhookSecret);

    ApiClient apiClient = new ApiClient(builder.build());
    apiClient.setBasePath(format("%s:%s", BASE_URL, port));
    return new WebhooksApi(apiClient);
  }

  private SupabaseDatabaseWebhook buildInsertPayload(UserTypeEnum userType) {
    var profileId = randomUUID();
    var now = now();

    SupabaseProfileRecordUserMetadata metadata =
        new SupabaseProfileRecordUserMetadata().userType(userType);

    SupabaseProfileRecord record =
        new SupabaseProfileRecord()
            .id(profileId)
            .email(
                format(
                    "test-%s-%d@example.com",
                    userType.getValue().toLowerCase(), currentTimeMillis()))
            .userMetadata(metadata)
            .appMetadata(Map.of())
            .createdAt(now)
            .updatedAt(now);

    SupabaseProfileRecord emptyOldRecord =
        new SupabaseProfileRecord()
            .id(randomUUID())
            .email("placeholder@example.com")
            .userMetadata(new SupabaseProfileRecordUserMetadata().userType(UserTypeEnum.RESEARCHER))
            .appMetadata(Map.of())
            .createdAt(now)
            .updatedAt(now);

    return new SupabaseDatabaseWebhook()
        .type(TypeEnum.INSERT)
        .table(TEST_TABLE)
        .schema(TEST_SCHEMA)
        .record(record)
        .oldRecord(emptyOldRecord);
  }
}
