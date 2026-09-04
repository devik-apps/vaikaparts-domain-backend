package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.mapper.exchange.DemandMapper;
import com.devikapps.vaikaparts.mapper.exchange.OfferMapper;
import com.devikapps.vaikaparts.mapper.user.ManagerMapper;
import com.devikapps.vaikaparts.mapper.user.ResearcherMapper;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.exchange.Exchange;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.model.user.User;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMapper {

  private final SellerMapper sellerMapper;
  private final ResearcherMapper researcherMapper;
  private final ManagerMapper managerMapper;
  private final DemandMapper demandMapper;
  private final OfferMapper offerMapper;

  public Notification toDomain(JDemandPublishedNotification notification) {
    if (notification == null) return null;

    return Notification.builder()
        .id(notification.getId())
        .notificationRequestedId(
            notification.getNotificationRequested() == null
                ? null
                : notification.getNotificationRequested().getId())
        .recipient(mapRecipient(notification.getRecipient()))
        .resource(mapResource(notification))
        .message(notification.getMessage())
        .notificationType(notification.getNotificationType())
        .read(notification.isRead())
        .clickAction(notification.getClickAction())
        .createdAt(notification.getCreatedAt())
        .readAt(notification.getReadAt())
        .build();
  }

  private User mapRecipient(JUser user) {
    return switch (user.getUserType()) {
      case SELLER -> sellerMapper.toSeller((JSeller) user);
      case RESEARCHER -> researcherMapper.toResearcher((JResearcher) user);
      case MANAGER -> managerMapper.toManager((JManager) user);
    };
  }

  private Exchange mapResource(JDemandPublishedNotification notification) {
    if (notification.getOffer() != null) return offerMapper.toDomain(notification.getOffer());
    if (notification.getDemand() != null) return demandMapper.toDomain(notification.getDemand());
    return null;
  }
}
