package com.devikapps.vaikaparts.exception;

public class NotificationBatchRequestedException extends RuntimeException {
  public NotificationBatchRequestedException(String message) {
    super(message);
  }

  public NotificationBatchRequestedException(String message, Throwable cause) {
    super(message, cause);
  }
}
