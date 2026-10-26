package com.devikapps.vaikaparts.service.notification.push;

import com.devikapps.vaikaparts.repository.model.notification.JWebPushSubscription;

/** Transport boundary: returns the provider HTTP status; never returns provider response bodies. */
@FunctionalInterface
public interface WebPushSender {
  int send(JWebPushSubscription subscription, byte[] payload) throws Exception;
}
