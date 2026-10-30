package com.devikapps.vaikaparts.mapper;

import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.model.user.Seller;
import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotification;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class NotificationMapperIT extends FacadeIT {

  @Autowired private EntityManager entityManager;
  @Autowired private NotificationMapper notificationMapper;

  @Test
  @Transactional
  void mapsSellerRecipientLoadedAsLazyBaseUserProxy() {
    var sellerId = randomUUID().toString();
    var createdAt = java.time.OffsetDateTime.now();
    entityManager.persist(
        JSeller.builder()
            .id(sellerId)
            .supabaseUserId(randomUUID().toString())
            .name("Seller")
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .build());
    entityManager.flush();
    entityManager.clear();

    var notification =
        JDemandPublishedNotification.builder()
            .id(randomUUID().toString())
            .recipient(entityManager.getReference(JUser.class, sellerId))
            .notificationType(NotificationType.SYSTEM_ANNOUNCEMENT)
            .message("Test")
            .createdAt(now())
            .build();

    var mapped = notificationMapper.toDomain(notification);

    assertInstanceOf(Seller.class, mapped.getRecipient());
  }
}
