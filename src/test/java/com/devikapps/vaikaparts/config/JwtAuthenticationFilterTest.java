package com.devikapps.vaikaparts.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.devikapps.vaikaparts.config.sec.AuthenticatedSupabaseUser;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import com.devikapps.vaikaparts.service.JwtValidationService;
import com.devikapps.vaikaparts.service.UserCreationService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterTest {
  private static final String TOKEN = "valid-jwt";
  private static final String SUBJECT = "supabase-user";

  @Mock private UserCreationService creationService;
  @Mock private JwtValidationService validationService;
  @Mock private UserRepository users;
  @InjectMocks private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    DecodedJWT jwt = JWT.decode(JWT.create()
        .withSubject(SUBJECT)
        .withClaim("email", "user@example.com")
        .withClaim("phone", "+123456789")
        .withClaim("app_metadata", Map.of("user_type", "SELLER"))
        .sign(Algorithm.HMAC256("test-secret")));
    when(validationService.validateToken(TOKEN)).thenReturn(Optional.of(jwt));
    when(creationService.extractMetadataValue(Map.of("user_type", "SELLER"), "user_type"))
        .thenReturn(Optional.of("SELLER"));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void suspendedLocalUserIsNotAuthenticated() throws Exception {
    localUser(UserStatus.SUSPENDED);

    authenticate(TOKEN);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void disabledLocalUserIsNotAuthenticated() throws Exception {
    localUser(UserStatus.DISABLED);

    authenticate(TOKEN);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void enabledLocalUserRetainsPrincipalAndRoles() throws Exception {
    localUser(UserStatus.ENABLED);

    authenticate(TOKEN);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
    assertEquals(SUBJECT, ((AuthenticatedSupabaseUser) authentication.getPrincipal()).id());
    assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SELLER")));
  }

  @Test
  void statusChangeRejectsSameJwtOnNextRequest() throws Exception {
    JUser user = localUser(UserStatus.ENABLED);
    authenticate(TOKEN);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());

    SecurityContextHolder.clearContext();
    user.setStatus(UserStatus.SUSPENDED);
    authenticate(TOKEN);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void absentLocalUserCanAuthenticateForLazyCreation() throws Exception {
    when(users.findBySupabaseUserId(SUBJECT)).thenReturn(Optional.empty());

    authenticate(TOKEN);

    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void missingTokenLeavesRequestUnauthenticated() throws Exception {
    authenticate(null);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void invalidTokenLeavesRequestUnauthenticated() throws Exception {
    when(validationService.validateToken("malformed")).thenReturn(Optional.empty());
    authenticate("malformed");
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  private JUser localUser(UserStatus status) {
    JUser user = JUser.builder().supabaseUserId(SUBJECT).status(status).build();
    when(users.findBySupabaseUserId(SUBJECT)).thenReturn(Optional.of(user));
    return user;
  }

  private void authenticate(String token) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
    if (token != null) request.addHeader("Authorization", "Bearer " + token);
    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
  }
}
