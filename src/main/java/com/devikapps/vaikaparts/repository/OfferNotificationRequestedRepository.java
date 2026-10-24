package com.devikapps.vaikaparts.repository;

import com.devikapps.vaikaparts.repository.event.JOfferNotificationRequested;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferNotificationRequestedRepository
    extends JpaRepository<JOfferNotificationRequested, String> {}
