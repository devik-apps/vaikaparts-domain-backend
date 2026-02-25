package com.devikapps.vaikaparts.service;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.exception.UserNotFoundException;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final List<NotificationChannel> channels;
  private final UserRepository userRepository;
  private final SellerMapper sellerMapper;
  private final DemandService demandService;

  public Notification createAndSendNotification(NotificationRequest request) {
    log.info("Creating notification for seller: {}", forJava(request.getSellerId()));

    var notification = buildNotification(request);
    sendThroughChannels(notification);

    log.info("Notification created and sent: {}", forJava(notification.getId()));
    return notification;
  }

  private Notification buildNotification(NotificationRequest request) {
    var jSeller =
        (JSeller)
            userRepository
                .findJUserById(request.getSellerId())
                .orElseThrow(
                    () ->
                        new UserNotFoundException(
                            format(
                                "No seller with id=%s not found", forJava(request.getSellerId()))));
    var seller = sellerMapper.toSeller(jSeller);

    return Notification.builder()
        .id(randomUUID().toString())
        .seller(seller)
        .notificationRequestedId(request.getNotificationRequestedId())
        .demand(demandService.getDemandByIdWithoutAuthFilter(request.getDemandId()))
        .message(request.getMessage())
        .notificationType(request.getNotificationType())
        .read(false)
        .clickAction(request.getClickAction())
        .createdAt(LocalDateTime.now())
        .build();
  }

  private void sendThroughChannels(Notification notification) {
    channels.stream()
        .filter(NotificationChannel::isEnabled)
        .forEach(
            channel -> {
              try {
                log.debug("Sending notification via channel: {}", channel.getChannelType());
                channel.send(notification);
              } catch (Exception e) {
                log.error(
                    "Failed to send notification via channel: {}", channel.getChannelType(), e);
              }
            });
  }
}
