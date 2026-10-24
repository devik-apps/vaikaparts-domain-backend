package com.devikapps.vaikaparts.service.notification.delivery;

import com.devikapps.vaikaparts.model.classifier.ProcessStatus;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.repository.DemandPublishedNotificationRepository;
import com.devikapps.vaikaparts.repository.ExternalNotificationDeliveryRepository;
import com.devikapps.vaikaparts.repository.event.JExternalNotificationDelivery;
import com.devikapps.vaikaparts.service.notification.NotificationChannel;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Adds external-channel work to the same transaction that persists the IN_APP notification. */
@Service
@RequiredArgsConstructor
public class ExternalNotificationDeliveryService {

  private final ExternalNotificationDeliveryRepository repository;
  private final DemandPublishedNotificationRepository notificationRepository;

  @Transactional(propagation = Propagation.MANDATORY)
  public void enqueue(Notification notification, List<NotificationChannel> channels) {
    var now = LocalDateTime.now();
    var persistedNotification = notificationRepository.getReferenceById(notification.getId());
    channels.forEach(
        channel ->
            repository.save(
                JExternalNotificationDelivery.builder()
                    .id(notification.getId() + ":" + channel.getChannelType())
                    .notification(persistedNotification)
                    .channel(channel.getChannelType())
                    .status(ProcessStatus.PENDING)
                    .attemptNb(0)
                    .nextAttemptAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build()));
  }
}
