package com.devikapps.vaikaparts.service;

import static com.devikapps.vaikaparts.conf.EnvConf.SUPABASE_JWT_SECRET;
import static com.devikapps.vaikaparts.conf.EnvConf.SUPABASE_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.config.SupabaseConf;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JwtValidationServiceIT extends FacadeIT {

  private static final String ISSUER = SUPABASE_URL + "/auth/v1";
  private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
  private static final String USER_EMAIL = "test@example.com";
  private static final String USER_TYPE_KEY = "user_type";
  private static final String USER_TYPE_BUYER = "BUYER";
  private static final String NAME_KEY = "name";
  private static final String NAME_VALUE = "Test User";
  private static final String INVALID_TOKEN = "invalid-token";
  private static final String NON_EXISTENT_KEY = "non_existent_key";
  private static final String WRONG_SECRET_KEY = "wrong-secret-key";
  private static final String WRONG_ISSUER = "https://wrong-issuer.com";
  private static final String MALFORMED_TOKEN = "this.is.not.a.valid.jwt.token";
  private static final String BLANK_STRING = "   ";
  private static final String EMPTY_STRING = "";
  private static final long TOKEN_EXPIRY_SECONDS = 3600L;

  @Autowired private JwtValidationService jwtValidationService;
  @Autowired private SupabaseConf supabaseConf;

  @BeforeEach
  void set_up() {
    jwtValidationService.init();
  }

  @Test
  void validate_token_should_return_decoded_jwt_when_token_is_valid() {
    String validToken = generateValidToken();

    Optional<DecodedJWT> result = jwtValidationService.validateToken(validToken);

    assertThat(result).isPresent();
    assertThat(result.get().getSubject()).isEqualTo(USER_ID);
    assertThat(result.get().getIssuer()).isEqualTo(ISSUER);
  }

  @Test
  void validate_token_should_return_empty_when_token_is_null() {
    Optional<DecodedJWT> result = jwtValidationService.validateToken(null);

    assertThat(result).isEmpty();
  }

  @Test
  void validate_token_should_return_empty_when_token_is_blank() {
    Optional<DecodedJWT> result = jwtValidationService.validateToken(BLANK_STRING);

    assertThat(result).isEmpty();
  }

  @Test
  void validate_token_should_return_empty_when_token_is_empty() {
    Optional<DecodedJWT> result = jwtValidationService.validateToken(EMPTY_STRING);

    assertThat(result).isEmpty();
  }

  @Test
  void validate_token_should_return_empty_when_token_is_expired() {
    String expiredToken = generateExpiredToken();

    Optional<DecodedJWT> result = jwtValidationService.validateToken(expiredToken);

    assertThat(result).isEmpty();
  }

  @Test
  void validate_token_should_return_empty_when_token_has_invalid_signature() {
    String tokenWithInvalidSignature = generateTokenWithInvalidSignature();

    Optional<DecodedJWT> result = jwtValidationService.validateToken(tokenWithInvalidSignature);

    assertThat(result).isEmpty();
  }

  @Test
  void validate_token_should_return_empty_when_token_has_invalid_issuer() {
    String tokenWithInvalidIssuer = generateTokenWithInvalidIssuer();

    Optional<DecodedJWT> result = jwtValidationService.validateToken(tokenWithInvalidIssuer);

    assertThat(result).isEmpty();
  }

  @Test
  void validate_token_should_return_empty_when_token_is_malformed() {
    Optional<DecodedJWT> result = jwtValidationService.validateToken(MALFORMED_TOKEN);

    assertThat(result).isEmpty();
  }

  @Test
  void extract_user_id_should_return_user_id_when_token_is_valid() {
    String validToken = generateValidToken();

    Optional<String> result = jwtValidationService.extractUserId(validToken);

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(USER_ID);
  }

  @Test
  void extract_user_id_should_return_empty_when_token_is_null() {
    Optional<String> result = jwtValidationService.extractUserId(null);

    assertThat(result).isEmpty();
  }

  @Test
  void extract_user_id_should_return_empty_when_token_is_invalid() {
    Optional<String> result = jwtValidationService.extractUserId(INVALID_TOKEN);

    assertThat(result).isEmpty();
  }

  @Test
  void extract_user_id_should_return_empty_when_token_is_expired() {
    String expiredToken = generateExpiredToken();

    Optional<String> result = jwtValidationService.extractUserId(expiredToken);

    assertThat(result).isEmpty();
  }

  @Test
  void extract_email_should_return_email_when_email_claim_exists() {
    String validToken = generateValidToken();
    DecodedJWT jwt = jwtValidationService.validateToken(validToken).orElseThrow();

    Optional<String> result = jwtValidationService.extractEmail(jwt);

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(USER_EMAIL);
  }

  @Test
  void extract_email_should_return_empty_when_email_claim_is_missing() {
    String tokenWithoutEmail = generateTokenWithoutEmail();
    DecodedJWT jwt = jwtValidationService.validateToken(tokenWithoutEmail).orElseThrow();

    Optional<String> result = jwtValidationService.extractEmail(jwt);

    assertThat(result).isEmpty();
  }

  @Test
  void extract_email_should_throw_illegal_argument_exception_when_jwt_is_null() {
    assertThatThrownBy(() -> jwtValidationService.extractEmail(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("JWT cannot be null");
  }

  @Test
  void extract_user_metadata_should_return_metadata_when_key_exists() {
    String validToken = generateValidTokenWithMetadata();
    DecodedJWT jwt = jwtValidationService.validateToken(validToken).orElseThrow();

    Optional<String> result = jwtValidationService.extractUserMetadata(jwt, USER_TYPE_KEY);

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(USER_TYPE_BUYER);
  }

  @Test
  void extract_user_metadata_should_return_metadata_when_multiple_keys_exist() {
    String validToken = generateValidTokenWithMetadata();
    DecodedJWT jwt = jwtValidationService.validateToken(validToken).orElseThrow();

    Optional<String> userType = jwtValidationService.extractUserMetadata(jwt, USER_TYPE_KEY);
    Optional<String> name = jwtValidationService.extractUserMetadata(jwt, NAME_KEY);

    assertThat(userType).isPresent();
    assertThat(userType.get()).isEqualTo(USER_TYPE_BUYER);
    assertThat(name).isPresent();
    assertThat(name.get()).isEqualTo(NAME_VALUE);
  }

  @Test
  void extract_user_metadata_should_return_empty_when_key_does_not_exist() {
    String validToken = generateValidTokenWithMetadata();
    DecodedJWT jwt = jwtValidationService.validateToken(validToken).orElseThrow();

    Optional<String> result = jwtValidationService.extractUserMetadata(jwt, NON_EXISTENT_KEY);

    assertThat(result).isEmpty();
  }

  @Test
  void extract_user_metadata_should_return_empty_when_user_metadata_claim_is_missing() {
    String validToken = generateValidToken();
    DecodedJWT jwt = jwtValidationService.validateToken(validToken).orElseThrow();

    Optional<String> result = jwtValidationService.extractUserMetadata(jwt, USER_TYPE_KEY);

    assertThat(result).isEmpty();
  }

  @Test
  void extract_user_metadata_should_throw_illegal_argument_exception_when_jwt_is_null() {
    assertThatThrownBy(() -> jwtValidationService.extractUserMetadata(null, USER_TYPE_KEY))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("JWT cannot be null");
  }

  @Test
  void extract_user_metadata_should_throw_illegal_argument_exception_when_key_is_null() {
    String validToken = generateValidToken();
    DecodedJWT jwt = jwtValidationService.validateToken(validToken).orElseThrow();

    assertThatThrownBy(() -> jwtValidationService.extractUserMetadata(jwt, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Metadata key cannot be null or empty");
  }

  @Test
  void extract_user_metadata_should_throw_illegal_argument_exception_when_key_is_blank() {
    String validToken = generateValidToken();
    DecodedJWT jwt = jwtValidationService.validateToken(validToken).orElseThrow();

    assertThatThrownBy(() -> jwtValidationService.extractUserMetadata(jwt, BLANK_STRING))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Metadata key cannot be null or empty");
  }

  @Test
  void extract_user_metadata_should_throw_illegal_argument_exception_when_key_is_empty() {
    String validToken = generateValidToken();
    DecodedJWT jwt = jwtValidationService.validateToken(validToken).orElseThrow();

    assertThatThrownBy(() -> jwtValidationService.extractUserMetadata(jwt, EMPTY_STRING))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Metadata key cannot be null or empty");
  }

  private String generateValidToken() {
    return JWT.create()
        .withSubject(USER_ID)
        .withIssuer(ISSUER)
        .withClaim("email", USER_EMAIL)
        .withExpiresAt(Date.from(Instant.now().plusSeconds(TOKEN_EXPIRY_SECONDS)))
        .sign(Algorithm.HMAC256(SUPABASE_JWT_SECRET));
  }

  private String generateTokenWithoutEmail() {
    return JWT.create()
        .withSubject(USER_ID)
        .withIssuer(ISSUER)
        .withExpiresAt(Date.from(Instant.now().plusSeconds(TOKEN_EXPIRY_SECONDS)))
        .sign(Algorithm.HMAC256(SUPABASE_JWT_SECRET));
  }

  private String generateValidTokenWithMetadata() {
    Map<String, Object> userMetadata = new HashMap<>();
    userMetadata.put(USER_TYPE_KEY, USER_TYPE_BUYER);
    userMetadata.put(NAME_KEY, NAME_VALUE);

    return JWT.create()
        .withSubject(USER_ID)
        .withIssuer(ISSUER)
        .withClaim("email", USER_EMAIL)
        .withClaim("user_metadata", userMetadata)
        .withExpiresAt(Date.from(Instant.now().plusSeconds(TOKEN_EXPIRY_SECONDS)))
        .sign(Algorithm.HMAC256(SUPABASE_JWT_SECRET));
  }

  private String generateExpiredToken() {
    return JWT.create()
        .withSubject(USER_ID)
        .withIssuer(ISSUER)
        .withClaim("email", USER_EMAIL)
        .withExpiresAt(Date.from(Instant.now().minusSeconds(TOKEN_EXPIRY_SECONDS)))
        .sign(Algorithm.HMAC256(SUPABASE_JWT_SECRET));
  }

  private String generateTokenWithInvalidSignature() {
    return JWT.create()
        .withSubject(USER_ID)
        .withIssuer(ISSUER)
        .withClaim("email", USER_EMAIL)
        .withExpiresAt(Date.from(Instant.now().plusSeconds(TOKEN_EXPIRY_SECONDS)))
        .sign(Algorithm.HMAC256(WRONG_SECRET_KEY));
  }

  private String generateTokenWithInvalidIssuer() {
    return JWT.create()
        .withSubject(USER_ID)
        .withIssuer(WRONG_ISSUER)
        .withClaim("email", USER_EMAIL)
        .withExpiresAt(Date.from(Instant.now().plusSeconds(TOKEN_EXPIRY_SECONDS)))
        .sign(Algorithm.HMAC256(SUPABASE_JWT_SECRET));
  }
}
