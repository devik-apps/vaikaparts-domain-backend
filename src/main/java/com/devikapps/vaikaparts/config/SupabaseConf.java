package com.devikapps.vaikaparts.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Configuration
public class SupabaseConf {
  @Value("${supabase.url}")
  private String url;

  @Value("${supabase.jwt-secret}")
  private String jwtSecret;

  @Value("${supabase.webhook-secret}")
  private String webhookSecret;
}
