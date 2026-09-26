package com.devikapps.vaikaparts.sms;

/** Provider-independent request. Phone-number adaptation belongs to the provider adapter. */
public record SmsMessage(String phoneNumber, String message) {}
