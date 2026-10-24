package com.devikapps.vaikaparts.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.devikapps.vaikaparts.config.sec.AuthenticatedSupabaseUser;
import com.devikapps.vaikaparts.service.JwtValidationService;
import com.devikapps.vaikaparts.service.UserCreationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;



@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final int BEARER_PREFIX_LENGTH = 7;
  private final UserCreationService userCreationService;
  private final JwtValidationService jwtValidationService;
  private static final String USER_TYPE_KEY = "user_type";

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    try {
      extractToken(request).ifPresent(token -> authenticateRequest(token, request));
    } catch (Exception e) {
      log.error("Authentication filter error: {}", e.getMessage(), e);
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }

  private Optional<String> extractToken(HttpServletRequest request) {
    String authHeader = request.getHeader(AUTHORIZATION_HEADER);

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) return Optional.empty();

    return Optional.of(authHeader.substring(BEARER_PREFIX_LENGTH));
  }

  private void authenticateRequest(String token, HttpServletRequest request) {
    jwtValidationService
        .validateToken(token)
        .ifPresent(
            jwt -> {
              var user = createPrincipal(jwt);
              UsernamePasswordAuthenticationToken authentication =
                  createAuthentication(user, request);
              SecurityContextHolder.getContext().setAuthentication(authentication);
              log.debug("User authenticated: {}", user.id());
            });
  }

  private AuthenticatedSupabaseUser createPrincipal(DecodedJWT jwt) {
    return new AuthenticatedSupabaseUser(
        jwt.getSubject(),
        jwt.getClaim("email").asString(),
        jwt.getClaim("phone").asString(),
        nullableClaimMap(jwt, "user_metadata"),
        nullableClaimMap(jwt, "app_metadata"));
  }

  private Map<String, Object> nullableClaimMap(DecodedJWT jwt, String claimName) {
    var claim = jwt.getClaim(claimName);
    return claim.isNull() ? null : claim.asMap();
  }

  private UsernamePasswordAuthenticationToken createAuthentication(
      AuthenticatedSupabaseUser user, HttpServletRequest request) {
    var appMetadata = user.appMetadata();
    List<GrantedAuthority> authorities = new ArrayList<>();
    var userType =   userCreationService.extractMetadataValue(appMetadata, USER_TYPE_KEY).orElse(null);
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    authorities.add(new SimpleGrantedAuthority("ROLE_"+userType));
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            user, null, authorities);
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    return authentication;
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI();
    return isPublicEndpoint(path);
  }

  private boolean isPublicEndpoint(String path) {
    return path.equals("/")
        || path.startsWith("/ping")
        || path.startsWith("/health")
        || path.startsWith("/webhooks")
        || path.startsWith("/doc")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/ws")
        || path.startsWith("/v3/api-docs");
  }
}
