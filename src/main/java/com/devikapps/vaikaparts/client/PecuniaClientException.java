package com.devikapps.vaikaparts.client;

public class PecuniaClientException extends RuntimeException {
  public PecuniaClientException(String message) {
    super(message);
  }

  public PecuniaClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
