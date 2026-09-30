package com.devikapps.vaikaparts.sms;

public interface SmsProvider {
  SmsReceipt send(SmsMessage message);
}
