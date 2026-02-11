package com.devikapps.vaikaparts.exception;

public class JwtClaimExtractionException extends RuntimeException {
  public JwtClaimExtractionException(String message) {
    super(message);
  }

  public JwtClaimExtractionException(String message, Throwable cause) {
    super(message, cause);
  }
}
