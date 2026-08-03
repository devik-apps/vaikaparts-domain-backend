package com.devikapps.vaikaparts.service;

import com.auth0.jwk.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.devikapps.vaikaparts.config.SupabaseConf;
import com.devikapps.vaikaparts.exception.JwtClaimExtractionException;
import jakarta.annotation.PostConstruct;

import java.net.URI;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtValidationService {

  private final SupabaseConf supabaseConf;
  private String issuer;
  private JwkProvider jwkProvider;

  @PostConstruct
  public void init() {
     this.issuer = supabaseConf.getUrl() + "/auth/v1";
    log.info("JWT verifier initialized successfully");
    try {
      URI jwksUri = URI.create(supabaseConf.getDiscoveryUrl());
      // UrlJwkProvider intègre nativement un mécanisme de mise en cache
      this.jwkProvider = new UrlJwkProvider(jwksUri.toURL());
      log.info("JWT verifiers (Hybrid HS256 & JWKS) initialized successfully");
    } catch (Exception e) {
      log.error("Failed to initialize JWK Provider for Supabase", e);
      throw new IllegalStateException("JWK Provider initialization failed", e);
    }
  }

  public Optional<DecodedJWT> validateToken(String token) {
    if (token == null || token.isBlank()) {
      log.debug("Token validation rejected: received null or empty token");
      return Optional.empty();
    }

    try {
      DecodedJWT decodedJwt = JWT.decode(token);
      DecodedJWT verifiedJwt;
        log.debug("Processing new RS256 asymmetric token");
        verifiedJwt = validateJWT(decodedJwt, token);
        log.debug("Token validation successful for user authentication");
      return Optional.of(verifiedJwt);
    } catch (TokenExpiredException e) {
      log.warn("Token validation failed: token has expired");
      return Optional.empty();
    } catch (SignatureVerificationException e) {
      log.warn("Token validation failed: invalid signature detected");
      return Optional.empty();
    } catch (AlgorithmMismatchException e) {
      log.warn("Token validation failed: algorithm mismatch");
      return Optional.empty();
    } catch (InvalidClaimException e) {
      log.warn("Token validation failed: invalid claim", e);
      return Optional.empty();
    } catch (JWTDecodeException e) {
      log.warn("Token validation failed: malformed token structure");
      return Optional.empty();
    } catch (InvalidPublicKeyException e) {
      log.warn("Token validation failed: invalid public key");
      return Optional.empty();
    } catch (JwkException e) {
        log.warn("Token validation failed: {}", e.getMessage());
      return Optional.empty();
    }
  }

  public Optional<String> extractUserId(String token) {
    Optional<DecodedJWT> decodedJWT = validateToken(token);
    if (decodedJWT.isEmpty()) {
      log.debug("User ID extraction failed: token validation unsuccessful");
      return Optional.empty();
    }

    String userId = decodedJWT.get().getSubject();
    log.debug("User ID extracted successfully from valid token");
    return Optional.ofNullable(userId);
  }

  public Optional<String> extractEmail(DecodedJWT jwt) {
    if (jwt == null) {
      log.error("Email extraction attempted with null JWT");
      throw new IllegalArgumentException("JWT cannot be null");
    }

    try {
      String email = jwt.getClaim("email").asString();
      if (email == null) {
        log.warn("Email extraction failed: email claim is missing in JWT");
        return Optional.empty();
      }
      log.debug("Email extracted successfully from JWT");
      return Optional.of(email);
    } catch (JWTDecodeException e) {
      log.error("Email extraction failed: unable to decode email claim");
      throw new JwtClaimExtractionException("Failed to decode email claim from JWT", e);
    } catch (ClassCastException e) {
      log.error("Email extraction failed: email claim has unexpected type");
      throw new JwtClaimExtractionException("Email claim has unexpected type", e);
    }
  }

  public Optional<String> extractUserMetadata(DecodedJWT jwt, String key) {
    if (jwt == null) {
      log.error("User metadata extraction attempted with null JWT");
      throw new IllegalArgumentException("JWT cannot be null");
    }

    if (key == null || key.isBlank()) {
      log.error("User metadata extraction attempted with null or empty key");
      throw new IllegalArgumentException("Metadata key cannot be null or empty");
    }

    try {
      Map<String, Object> userMetadata = jwt.getClaim("user_metadata").asMap();
      if (userMetadata == null) {
        log.debug("User metadata extraction failed: user_metadata claim is missing");
        return Optional.empty();
      }

      Object metadata = userMetadata.get(key);
      if (metadata == null) {
        log.debug("User metadata extraction: key '{}' not found in user_metadata claim", key);
        return Optional.empty();
      }

      log.debug("User metadata extracted successfully for key: {}", key);
      return Optional.of(metadata.toString());
    } catch (JWTDecodeException e) {
      log.error(
          "User metadata extraction failed for key '{}': unable to decode user_metadata claim",
          key);
      throw new JwtClaimExtractionException("Failed to decode user_metadata claim from JWT", e);
    } catch (ClassCastException e) {
      log.error(
          "User metadata extraction failed for key '{}': user_metadata claim has unexpected type",
          key);
      throw new JwtClaimExtractionException("User metadata claim has unexpected type", e);
    }
  }

  public DecodedJWT validateJWT(DecodedJWT jwt,String token) throws JwkException{
    Jwk jwk = jwkProvider.get(jwt.getKeyId());
    ECPublicKey publicKey = (ECPublicKey) jwk.getPublicKey();

    Algorithm algorithm = Algorithm.ECDSA256(publicKey, null);
    JWTVerifier verifier = JWT.require(algorithm).withIssuer(issuer)
            .withAudience("authenticated") // Recommandé par Supabase
            .build();
    return verifier.verify(token);
  }
}
