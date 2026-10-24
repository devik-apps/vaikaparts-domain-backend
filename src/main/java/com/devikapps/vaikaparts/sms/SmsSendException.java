package com.devikapps.vaikaparts.sms;

/** Does not expose the API key, recipient or raw provider response. */
public class SmsSendException extends RuntimeException {
  public enum Reason {
    INVALID_REQUEST,
    AUTHENTICATION,
    ACCOUNT_UNAVAILABLE,
    RATE_LIMIT,
    PROVIDER_ERROR,
    UNKNOWN_OUTCOME
  }

  private final Reason reason;
  private final Integer httpStatus;

  public SmsSendException(Reason reason, Integer httpStatus) {
    super("SMS submission failed: " + reason + (httpStatus == null ? "" : " (HTTP " + httpStatus + ")"));
    this.reason = reason;
    this.httpStatus = httpStatus;
  }

  public Reason getReason() { return reason; }

  public Integer getHttpStatus() { return httpStatus; }
}
