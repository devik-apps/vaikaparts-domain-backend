package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.event.JNotificationRequested;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRequestedRepository
    extends JpaRepository<JNotificationRequested, String> {}
