package com.devikapps.vaikaparts.exception;

public class DemandPublishedNotificationRequestedException extends RuntimeException {
  public DemandPublishedNotificationRequestedException(String message) {
    super(message);
  }

  public DemandPublishedNotificationRequestedException(String message, Throwable cause) {
    super(message, cause);
  }
}
