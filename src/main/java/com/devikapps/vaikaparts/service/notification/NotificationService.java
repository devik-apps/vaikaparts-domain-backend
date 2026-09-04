package com.devikapps.vaikaparts.service.notification;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.exception.UserNotFoundException;
import com.devikapps.vaikaparts.mapper.NotificationMapper;
import com.devikapps.vaikaparts.mapper.user.ManagerMapper;
import com.devikapps.vaikaparts.mapper.user.ResearcherMapper;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.exchange.Exchange;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.model.user.User;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import com.devikapps.vaikaparts.service.DemandService;
import com.devikapps.vaikaparts.service.OfferService;
import com.devikapps.vaikaparts.service.UserService;
import com.devikapps.vaikaparts.service.util.Paginator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final List<NotificationChannel> channels;
  private final UserRepository userRepository;
  private final SellerMapper sellerMapper;
  private final ResearcherMapper researcherMapper;
  private final ManagerMapper managerMapper;
  private final DemandService demandService;
  private final OfferService offerService;
  private final DemandPublishedNotificationRepository demandPublishedNotificationRepository;
  private final Paginator paginator;
  private final UserService userService;
  private final NotificationMapper notificationMapper;

  public Notification createAndSendNotification(NotificationRequest request) {
    var notificationType =
        Objects.requireNonNull(request.getNotificationType(), "Notification type is required");
    log.info(
        "Creating notification type={} for recipient user={}",
        forJava(notificationType.toString()),
        forJava(request.getRecipientUserId()));

    var notification = buildNotification(request);
    sendThroughChannels(notification);

    log.info(
        "Notification created and sent: id={}, recipientType={}",
        forJava(notification.getId()),
        forJava(notification.getRecipient().getUserType().toString()));
    return notification;
  }

  @Transactional(readOnly = true)
  public Page<Notification> fetchAllNotification(Integer page, Integer size) {
    var pagination = paginator.apply(page, size);
    var currentUser = userService.getCurrentUser();
    var fPage = pagination.get("page");
    var fSize = pagination.get("size");

    var pageable = PageRequest.of(fPage, fSize);
    log.info(
        "Fetching notifications for current user={} ({}) with page={}, size={}",
        forJava(currentUser.getId()),
        currentUser.getUserType(),
        fPage,
        fSize);

    return demandPublishedNotificationRepository
        .findByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
        .map(notificationMapper::toDomain);
  }

  @Transactional(readOnly = true)
  public Notification getNotification(@NotNull @NotBlank String notificationId) {
    var currentUser = userService.getCurrentUser();
    var notification =
        demandPublishedNotificationRepository
            .findByIdAndRecipientId(notificationId, currentUser.getId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        format(
                            "No notification found with given id=%s to fetch",
                            forJava(notificationId))));

    return notificationMapper.toDomain(notification);
  }

  @Transactional
  public Notification markAsRead(@NotNull @NotBlank String notificationId) {
    log.info(
        "Processing marking notification with notification id={} as read", forJava(notificationId));
    var currentUser = userService.getCurrentUser();
    var notification =
        demandPublishedNotificationRepository
            .findByIdAndRecipientId(notificationId, currentUser.getId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        format(
                            "No notification found with given id=%s to mark as read",
                            forJava(notificationId))));

    log.info("Notification read state : {}", notification.isRead());
    notification.setRead(true);
    notification.setReadAt(LocalDateTime.now());

    var persisted = demandPublishedNotificationRepository.save(notification);
    log.info("Notification read state after processing : {}", persisted.isRead());
    return notificationMapper.toDomain(persisted);
  }

  private Notification buildNotification(NotificationRequest request) {
    var jUser =
        userRepository
            .findJUserById(request.getRecipientUserId())
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        format(
                            "No user with id=%s was found",
                            forJava(request.getRecipientUserId()))));

    var recipient = mapRecipient(jUser);
    var resource = handleNotificationType(request, recipient.getUserType());

    return Notification.builder()
        .id(randomUUID().toString())
        .recipient(recipient)
        .notificationRequestedId(request.getNotificationRequestedId())
        .resource(resource)
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
                log.debug(
                    "Sending notification type={} to recipientType={} via channel={}",
                    notification.getNotificationType(),
                    notification.getRecipient().getUserType(),
                    channel.getChannelType());
                channel.send(notification);
              } catch (Exception e) {
                log.error(
                    "Failed to send notification via channel: {}", channel.getChannelType(), e);
              }
            });
  }

  private User mapRecipient(JUser jUser) {
    return switch (jUser.getUserType()) {
      case SELLER -> sellerMapper.toSeller((JSeller) jUser);
      case RESEARCHER -> researcherMapper.toResearcher((JResearcher) jUser);
      case MANAGER -> managerMapper.toManager((JManager) jUser);
    };
  }

  private Exchange handleNotificationType(NotificationRequest request, UserType recipientUserType) {
    var notificationType =
        Objects.requireNonNull(request.getNotificationType(), "Notification type is required");
    validateRecipientType(notificationType, recipientUserType);

    return switch (notificationType) {
      case DEMAND_PUBLISHED, DEMAND_CANCELED ->
          demandService.getDemandByIdWithoutAuthFilter(requireResourceId(request));
      case OFFER_ACCEPTED, OFFER_REJECTED ->
          offerService.getOfferByIdWithoutAuthFilter(requireResourceId(request));
      case SYSTEM_ANNOUNCEMENT -> null;
    };
  }

  private void validateRecipientType(
      NotificationType notificationType, UserType recipientUserType) {
    boolean supported =
        switch (notificationType) {
          case SYSTEM_ANNOUNCEMENT, DEMAND_CANCELED -> true;
          case DEMAND_PUBLISHED, OFFER_ACCEPTED, OFFER_REJECTED ->
              recipientUserType == UserType.SELLER || recipientUserType == UserType.MANAGER;
        };

    if (!supported) {
      throw new IllegalArgumentException(
          format(
              "Notification type %s cannot be sent to user type %s",
              notificationType, recipientUserType));
    }
  }

  private String requireResourceId(NotificationRequest request) {
    if (request.getResourceId() == null || request.getResourceId().isBlank()) {
      throw new IllegalArgumentException(
          format(
              "A resource id is required for notification type %s", request.getNotificationType()));
    }
    return request.getResourceId();
  }
}
