package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.event.JDemandPublishedNotificationRequested;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRequestedRepository
    extends JpaRepository<JDemandPublishedNotificationRequested, String> {}
