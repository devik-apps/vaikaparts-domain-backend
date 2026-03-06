package com.devikapps.vaikaparts.client;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.devikapps.vaikaparts.conf.EnvConf;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class JwtTestFactory {

  private JwtTestFactory() {
    throw new UnsupportedOperationException("UTILITY CLASS, NOT MEANT TO BE INSTANCED");
  }

  public static String generateToken(String supabaseUserId) {
    Algorithm algorithm = Algorithm.HMAC256(EnvConf.SUPABASE_JWT_SECRET);
    String issuer = EnvConf.SUPABASE_URL + "/auth/v1";

    return JWT.create()
        .withSubject(supabaseUserId)
        .withIssuer(issuer)
        .withIssuedAt(Instant.now())
        .withExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
        .sign(algorithm);
  }
}
