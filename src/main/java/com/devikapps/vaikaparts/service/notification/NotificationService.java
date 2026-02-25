package com.devikapps.vaikaparts.service.notification;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.exception.UserNotFoundException;
import com.devikapps.vaikaparts.mapper.NotificationMapper;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.NotificationRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.DemandService;
import com.devikapps.vaikaparts.service.NotificationChannel;
import com.devikapps.vaikaparts.service.SellerService;
import com.devikapps.vaikaparts.service.util.Paginator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final List<NotificationChannel> channels;
  private final UserRepository userRepository;
  private final SellerMapper sellerMapper;
  private final DemandService demandService;
  private final NotificationRepository notificationRepository;
  private final Paginator paginator;
  private final SellerService sellerService;
  private final NotificationMapper notificationMapper;

  public Notification createAndSendNotification(NotificationRequest request) {
    log.info("Creating notification for seller: {}", forJava(request.getSellerId()));

    var notification = buildNotification(request);
    sendThroughChannels(notification);

    log.info("Notification created and sent: {}", forJava(notification.getId()));
    return notification;
  }

  public Page<Notification> fetchAllNotification(Integer page, Integer size) {
    var pagination = paginator.apply(page, size);
    var currentActiveSeller = sellerService.getCurrentSeller();
    var fPage = pagination.get("page");
    var fSize = pagination.get("size");

    var pageable = PageRequest.of(fPage, fSize);
    log.info(
        "Fetching all notification of current active seller with page={}, size={}", fPage, fSize);

    return notificationRepository
        .findBySellerIdOrderByCreatedAtDesc(currentActiveSeller.getId(), pageable)
        .map(notificationMapper::toDomain);
  }

  public Notification getNotification(@NotNull @NotBlank String notificationId) {
    var currentActiveSeller = sellerService.getCurrentSeller();
    var notification =
        notificationRepository
            .findByIdAndSellerId(notificationId, currentActiveSeller.getId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        format(
                            "No authentication found with given id=%s", forJava(notificationId))));

    return notificationMapper.toDomain(notification);
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
