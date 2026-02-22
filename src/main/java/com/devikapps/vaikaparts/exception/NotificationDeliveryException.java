package com.devikapps.vaikaparts.exception;

public class NotificationDeliveryException extends RuntimeException {

  public NotificationDeliveryException(String message) {
    super(message);
  }

  public NotificationDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
