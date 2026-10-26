package com.devikapps.vaikaparts.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.devikapps.vaikaparts.config.JacksonConf;
import com.devikapps.vaikaparts.config.JwtAuthenticationFilter;
import com.devikapps.vaikaparts.config.sec.SecurityConf;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.service.JwtValidationService;
import com.devikapps.vaikaparts.service.UserCreationService;
import com.devikapps.vaikaparts.service.notification.WebPushProperties;
import com.devikapps.vaikaparts.service.notification.push.WebPushSubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WebPushSubscriptionController.class)
@ContextConfiguration(
    classes = {
      WebPushSubscriptionController.class,
      WebPushProperties.class,
      JacksonConf.class,
      SecurityConf.class,
      JwtAuthenticationFilter.class
    })
class WebPushSubscriptionSecurityTest {
  private static final String BASE = "/v1/notifications/push-subscriptions";
  private static final String PAYLOAD =
      "{\"endpoint\":\"https://8.8.8.8/push/test\",\"keys\":{\"p256dh\":\"key\",\"auth\":\"auth\"}}";
  @Autowired private MockMvc mvc;
  @Autowired private WebPushProperties properties;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private WebPushSubscriptionService subscriptions;
  @MockitoBean private JwtValidationService jwtValidation;
  @MockitoBean private UserCreationService userCreation;
  @MockitoBean private UserRepository users;
  private String token;

  @BeforeEach
  void setup() {
    properties.setEnabled(true);
    properties.setPublicKey("public-base64url");
    properties.setPrivateKey("never-expose-this-private-key");
    token =
        JWT.create()
            .withSubject("supabase-slice-user")
            .withClaim("app_metadata", Map.of("user_type", "RESEARCHER"))
            .sign(Algorithm.HMAC256("slice-test-secret"));
    when(jwtValidation.validateToken(token)).thenReturn(Optional.of(JWT.decode(token)));
    when(users.findBySupabaseUserId("supabase-slice-user")).thenReturn(Optional.empty());
    when(userCreation.extractMetadataValue(anyMap(), eq("user_type")))
        .thenReturn(Optional.of("RESEARCHER"));
  }

  @Test
  void subscriptionRoutesRequireAuthentication() throws Exception {
    mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(PAYLOAD))
        .andExpect(status().isUnauthorized());
    mvc.perform(delete(BASE).contentType(APPLICATION_JSON).content(PAYLOAD))
        .andExpect(status().isUnauthorized());
    mvc.perform(get(BASE + "/vapid-public-key")).andExpect(status().isUnauthorized());
    verifyNoInteractions(subscriptions);
  }

  @Test
  void authenticatedPublicKeySerializesOnlyPublicKey() throws Exception {
    var response =
        mvc.perform(get(BASE + "/vapid-public-key").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var json = objectMapper.readTree(response);
    assertEquals(1, json.size());
    assertEquals("public-base64url", json.get("publicKey").asText());
    assertFalse(response.contains(properties.getPrivateKey()));
  }

  @Test
  void disabledPublicKeyReturnsServiceUnavailable() throws Exception {
    properties.setEnabled(false);
    var response =
        mvc.perform(get(BASE + "/vapid-public-key").header("Authorization", "Bearer " + token))
            .andExpect(status().isServiceUnavailable())
            .andReturn()
            .getResponse();
    assertFalse(response.getContentAsString().contains(properties.getPrivateKey()));
  }

  @Test
  void absentPublicKeyReturnsServiceUnavailable() throws Exception {
    properties.setPublicKey("");
    mvc.perform(get(BASE + "/vapid-public-key").header("Authorization", "Bearer " + token))
        .andExpect(status().isServiceUnavailable());
  }
}
