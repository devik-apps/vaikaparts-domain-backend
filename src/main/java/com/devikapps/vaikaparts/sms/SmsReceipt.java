package com.devikapps.vaikaparts.sms;

/** The provider accepted the request; this is not a handset delivery confirmation. */
public record SmsReceipt(String providerMessageId) {}
