package com.devikapps.vaikaparts.config;

import com.devikapps.vaikaparts.service.JwtValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

  private final JwtValidationService jwtValidationService;

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
              String userId = jwt.getSubject();
              UsernamePasswordAuthenticationToken authentication =
                  createAuthentication(userId, request);
              SecurityContextHolder.getContext().setAuthentication(authentication);
              log.debug("User authenticated: {}", userId);
            });
  }

  private UsernamePasswordAuthenticationToken createAuthentication(
      String userId, HttpServletRequest request) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
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
        || path.startsWith("/v3/api-docs");
  }
}
