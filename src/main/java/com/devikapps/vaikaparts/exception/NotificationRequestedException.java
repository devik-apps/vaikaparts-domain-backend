package com.devikapps.vaikaparts.exception;

public class NotificationRequestedException extends RuntimeException {
  public NotificationRequestedException(String message) {
    super(message);
  }

  public NotificationRequestedException(String message, Throwable cause) {
    super(message, cause);
  }
}
