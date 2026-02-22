package com.devikapps.vaikaparts.service;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.model.notification.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {

  public void sendNotificationToSeller(String sellerId, Notification notification) {
    log.info("WebSocket: Sending notification to seller: {}", forJava(sellerId));

    // TODO: WebSocket will be implemented later.

    log.debug("WebSocket delivery placeholder - notification: {}", forJava(notification.getId()));
  }
}
