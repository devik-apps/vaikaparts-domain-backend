package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.event.JExternalNotificationDelivery;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalNotificationDeliveryRepository
    extends JpaRepository<JExternalNotificationDelivery, String>,
        ExternalNotificationDeliveryRepositoryCustom {

  List<JExternalNotificationDelivery> findAllByLockTokenOrderByCreatedAt(String lockToken);
}
