package com.devikapps.vaikaparts.exception;

import com.devikapps.vaikaparts.InfraGenerated;

@InfraGenerated
public class EmailSendException extends RuntimeException {
  public EmailSendException(String message) {
    super(message);
  }

  public EmailSendException(String message, Throwable cause) {
    super(message, cause);
  }
}
