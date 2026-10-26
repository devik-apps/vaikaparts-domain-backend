package com.devikapps.vaikaparts.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.WebPushSubscriptionRepository;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import com.devikapps.vaikaparts.service.notification.WebPushProperties;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = true)
class WebPushSubscriptionControllerIT extends FacadeIT {
  private static final String BASE = "/v1/notifications/push-subscriptions";
  private static final String ENDPOINT = "https://8.8.8.8/push/integration";
  private static final String PAYLOAD =
      "{\"endpoint\":\"" + ENDPOINT + "\",\"keys\":{\"p256dh\":\"key\",\"auth\":\"auth\"}}";
  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired WebPushSubscriptionRepository subscriptions;
  @Autowired WebPushProperties properties;
  private final java.util.ArrayList<String> userIds = new java.util.ArrayList<>();

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    subscriptions.deleteAll();
    userIds.forEach(users::deleteById);
    properties.setEnabled(false);
    properties.setPublicKey("");
    properties.setPrivateKey("");
  }

  @Test
  void subscriptionRoutesRequireAuthentication() throws Exception {
    SecurityContextHolder.clearContext();
    mvc.perform(post(BASE).contentType(APPLICATION_JSON).content(PAYLOAD))
        .andExpect(status().isUnauthorized());
    mvc.perform(delete(BASE).contentType(APPLICATION_JSON).content(PAYLOAD))
        .andExpect(status().isUnauthorized());
    mvc.perform(get(BASE + "/vapid-public-key")).andExpect(status().isUnauthorized());
  }

  @Test
  void registerAndDeleteUseAuthenticatedOwner() throws Exception {
    var owner = authenticateNewUser();
    mvc.perform(post(BASE).with(asCurrentUser()).contentType(APPLICATION_JSON).content(PAYLOAD))
        .andExpect(status().isNoContent());
    assertEquals(
        owner.getId(), subscriptions.findByEndpoint(ENDPOINT).orElseThrow().getUser().getId());
    var original = subscriptions.findByEndpoint(ENDPOINT).orElseThrow();
    authenticateNewUser();
    mvc.perform(post(BASE).with(asCurrentUser()).contentType(APPLICATION_JSON).content(PAYLOAD))
        .andExpect(status().isBadRequest());
    mvc.perform(delete(BASE).with(asCurrentUser()).contentType(APPLICATION_JSON).content(PAYLOAD))
        .andExpect(status().isNoContent());
    assertEquals(original.getId(), subscriptions.findByEndpoint(ENDPOINT).orElseThrow().getId());
    authenticate(owner);
    mvc.perform(delete(BASE).with(asCurrentUser()).contentType(APPLICATION_JSON).content(PAYLOAD))
        .andExpect(status().isNoContent());
    mvc.perform(delete(BASE).with(asCurrentUser()).contentType(APPLICATION_JSON).content(PAYLOAD))
        .andExpect(status().isNoContent());
    assertTrue(subscriptions.findByEndpoint(ENDPOINT).isEmpty());
  }

  @Test
  void publicKeyResponseNeverContainsPrivateKey() throws Exception {
    authenticateNewUser();
    properties.setPrivateKey("private-secret");
    properties.setPublicKey("public-value");
    properties.setEnabled(true);
    mvc.perform(get(BASE + "/vapid-public-key").with(asCurrentUser()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.publicKey").value("public-value"))
        .andExpect(content().json("{\"publicKey\":\"public-value\"}", true));
    properties.setEnabled(false);
    mvc.perform(get(BASE + "/vapid-public-key").with(asCurrentUser()))
        .andExpect(status().isServiceUnavailable());
    properties.setEnabled(true);
    properties.setPublicKey("");
    mvc.perform(get(BASE + "/vapid-public-key").with(asCurrentUser()))
        .andExpect(status().isServiceUnavailable());
  }

  private JUser authenticateNewUser() {
    var id = UUID.randomUUID().toString();
    var now = OffsetDateTime.now();
    var user =
        users.saveAndFlush(
            JUser.builder()
                .id(id)
                .supabaseUserId("supabase-" + id)
                .name("Push Test")
                .email(id + "@test.invalid")
                .phoneNumber("")
                .profileImgKey("")
                .userType(UserType.RESEARCHER)
                .status(UserStatus.ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build());
    userIds.add(id);
    authenticate(user);
    return user;
  }

  private JUser currentUser;

  private void authenticate(JUser user) {
    currentUser = user;
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user.getSupabaseUserId(), null, List.of()));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor asCurrentUser() {
    return org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.authentication(
        new UsernamePasswordAuthenticationToken(currentUser.getSupabaseUserId(), null, List.of()));
  }
}
