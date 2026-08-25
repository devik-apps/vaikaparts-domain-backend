package com.devikapps.vaikaparts.config.sec;

import java.util.Map;

public record AuthenticatedSupabaseUser(
    String id,
    String email,
    String phoneNumber,
    Map<String, Object> userMetadata,
    Map<String, Object> appMetadata) {}
