package com.devikapps.vaikaparts.client;

public class PecuniaUnknownOutcomeException extends PecuniaClientException {
  public PecuniaUnknownOutcomeException(String message) {
    super(message);
  }

  public PecuniaUnknownOutcomeException(String message, Throwable cause) {
    super(message, cause);
  }
}
