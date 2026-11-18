package com.devikapps.vaikaparts.mapper;

import com.devikapps.vaikaparts.mapper.exchange.DemandMapper;
import com.devikapps.vaikaparts.mapper.exchange.OfferMapper;
import com.devikapps.vaikaparts.mapper.user.ManagerMapper;
import com.devikapps.vaikaparts.mapper.user.ResearcherMapper;
import com.devikapps.vaikaparts.mapper.user.SellerMapper;
import com.devikapps.vaikaparts.model.exchange.Exchange;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Offer;
import com.devikapps.vaikaparts.model.notification.Notification;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.model.user.User;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
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
            notification.getOfferNotificationRequested() != null
                ? notification.getOfferNotificationRequested().getId()
                : notification.getNotificationRequested() == null
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
    var recipient = (JUser) Hibernate.unproxy(user);
    return switch (recipient.getUserType()) {
      case SELLER -> sellerMapper.toSeller((JSeller) recipient);
      case RESEARCHER -> researcherMapper.toResearcher((JResearcher) recipient);
      case MANAGER -> managerMapper.toManager((JManager) recipient);
    };
  }

  private Exchange mapResource(JDemandPublishedNotification notification) {
    boolean sellerRecipient =
        notification.getRecipient() != null
            && ((JUser) Hibernate.unproxy(notification.getRecipient())).getUserType()
                == com.devikapps.vaikaparts.model.classifier.UserType.SELLER;
    if (notification.getOffer() != null) {
      var offer = offerMapper.toDomain(notification.getOffer());
      if (sellerRecipient && offer.getDemand() != null) hideResearcherContact(offer.getDemand());
      return offer;
    }
    if (notification.getDemand() != null) {
      var demand = demandMapper.toDomain(notification.getDemand());
      if (sellerRecipient) hideResearcherContact(demand);
      return demand;
    }
    return null;
  }

  private void hideResearcherContact(Demand demand) {
    Researcher researcher = demand.getResearcher();
    if (researcher == null) return;
    researcher.setSupabaseUserId(null);
    researcher.setName(null);
    researcher.setEmail(null);
    researcher.setPhoneNumber(null);
    researcher.setProfileImgUrl(null);
  }
}
