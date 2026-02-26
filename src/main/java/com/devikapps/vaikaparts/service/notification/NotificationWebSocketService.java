package com.devikapps.vaikaparts.service.notification;

import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.model.notification.DemandPublishedNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {

  public void sendNotificationToSeller(
      String sellerId, DemandPublishedNotification demandPublishedNotification) {
    log.info("WebSocket: Sending notification to seller: {}", forJava(sellerId));

    // TODO: WebSocket will be implemented later.

    log.debug(
        "WebSocket delivery placeholder - notification: {}",
        forJava(demandPublishedNotification.getId()));
  }
}
