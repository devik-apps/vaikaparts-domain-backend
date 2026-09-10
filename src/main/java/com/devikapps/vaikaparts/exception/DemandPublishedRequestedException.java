package com.devikapps.vaikaparts.exception;

public class DemandPublishedRequestedException extends RuntimeException {
  public DemandPublishedRequestedException(String message) {
    super(message);
  }

  public DemandPublishedRequestedException(String message, Throwable cause) {
    super(message, cause);
  }
}
