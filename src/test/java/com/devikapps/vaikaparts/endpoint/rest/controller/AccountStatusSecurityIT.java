package com.devikapps.vaikaparts.endpoint.rest.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import com.devikapps.vaikaparts.service.JwtValidationService;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = true)
class AccountStatusSecurityIT extends FacadeIT {
  private static final String TOKEN = "valid-token";

  @Autowired private MockMvc mvc;
  @Autowired private UserRepository users;
  @MockitoBean private JwtValidationService jwtValidationService;

  @AfterEach
  void cleanUp() {
    users.deleteAll();
  }

  @ParameterizedTest
  @EnumSource(value = UserStatus.class, names = {"SUSPENDED", "DISABLED"})
  void researcherWithBlockedStatusReceives401OnProfileAndDemands(UserStatus status) throws Exception {
    JUser user = persist(UserType.RESEARCHER, status);
    validJwt(user);

    mvc.perform(get("/users/me").header("Authorization", "Bearer " + TOKEN))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/v1/demands").header("Authorization", "Bearer " + TOKEN))
        .andExpect(status().isUnauthorized());
    verify(jwtValidationService, org.mockito.Mockito.times(2)).validateToken(TOKEN);
  }

  @ParameterizedTest
  @EnumSource(value = UserStatus.class, names = {"SUSPENDED", "DISABLED"})
  void sellerWithBlockedStatusReceives401OnProfileAndOffers(UserStatus status) throws Exception {
    JUser user = persist(UserType.SELLER, status);
    validJwt(user);

    mvc.perform(get("/users/me").header("Authorization", "Bearer " + TOKEN))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/v1/offers").header("Authorization", "Bearer " + TOKEN))
        .andExpect(status().isUnauthorized());
    verify(jwtValidationService, org.mockito.Mockito.times(2)).validateToken(TOKEN);
  }

  @ParameterizedTest
  @EnumSource(value = UserType.class, names = {"RESEARCHER", "SELLER"})
  void enabledLocalUserCanReadProfile(UserType type) throws Exception {
    JUser user = persist(type, UserStatus.ENABLED);
    validJwt(user);

    mvc.perform(get("/users/me").header("Authorization", "Bearer " + TOKEN))
        .andExpect(status().isOk());
    verify(jwtValidationService).validateToken(TOKEN);
  }

  @Test
  void statusChangeRejectsSameJwtOnNextHttpRequest() throws Exception {
    JUser user = persist(UserType.RESEARCHER, UserStatus.ENABLED);
    validJwt(user);
    mvc.perform(get("/users/me").header("Authorization", "Bearer " + TOKEN))
        .andExpect(status().isOk());

    user.setStatus(UserStatus.SUSPENDED);
    users.saveAndFlush(user);
    mvc.perform(get("/users/me").header("Authorization", "Bearer " + TOKEN))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void missingJwtReceives401() throws Exception {
    mvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void malformedJwtReceives401() throws Exception {
    when(jwtValidationService.validateToken("malformed")).thenReturn(Optional.empty());
    mvc.perform(get("/users/me").header("Authorization", "Bearer malformed"))
        .andExpect(status().isUnauthorized());
    verify(jwtValidationService).validateToken("malformed");
  }

  @Test
  void pingStaysPublicWithoutJwt() throws Exception {
    mvc.perform(get("/ping")).andExpect(status().isOk());
  }

  private JUser persist(UserType type, UserStatus status) {
    OffsetDateTime now = OffsetDateTime.now();
    String id = UUID.randomUUID().toString();
    String subject = "subject-" + id;
    JUser user = type == UserType.RESEARCHER
        ? JResearcher.builder().id(id).supabaseUserId(subject).name("Test Researcher")
            .email("researcher@example.com").userType(type).status(status)
            .createdAt(now).updatedAt(now).build()
        : JSeller.builder().id(id).supabaseUserId(subject).name("Test Seller")
            .email("seller@example.com").userType(type).status(status)
            .createdAt(now).updatedAt(now).build();
    return users.saveAndFlush(user);
  }

  private void validJwt(JUser user) {
    DecodedJWT jwt = JWT.decode(JWT.create().withSubject(user.getSupabaseUserId())
        .withClaim("email", user.getEmail())
        .withClaim("app_metadata", Map.of("user_type", user.getUserType().name()))
        .sign(Algorithm.HMAC256("test-secret")));
    when(jwtValidationService.validateToken(TOKEN)).thenReturn(Optional.of(jwt));
  }
}
