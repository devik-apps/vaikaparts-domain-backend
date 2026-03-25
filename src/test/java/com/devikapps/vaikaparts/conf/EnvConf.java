package com.devikapps.vaikaparts.conf;

import static java.util.UUID.randomUUID;

import com.devikapps.vaikaparts.InfraGenerated;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;

@InfraGenerated
@TestConfiguration
public class EnvConf {

  public static final String SUPABASE_URL = "https://test.supabase.co";
  public static final String SUPABASE_JWT_SECRET = "test-secret-key-for-testing-purposes";
  public static final String SUPABASE_WEBHOOK_SECRET = "test-webhook-secret";
  public static final String PAYMENT_BASE_URL = "https://test.payment.com";
  public static final String PAYMENT_API_KEY = randomUUID().toString();

  public void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.profiles.active", () -> "test");
    registry.add("supabase.url", () -> SUPABASE_URL);
    registry.add("supabase.jwt-secret", () -> SUPABASE_JWT_SECRET);
    registry.add("supabase.webhook-secret", () -> SUPABASE_WEBHOOK_SECRET);

    registry.add("payment.base-url", () -> PAYMENT_BASE_URL);
    registry.add("payment.api-key", () -> PAYMENT_API_KEY);
  }
}
