package com.devikapps.vaikaparts.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.UserLanguage;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Part;
import com.devikapps.vaikaparts.service.notification.NotificationMessageResolver;
import java.time.Year;
import org.junit.jupiter.api.Test;

class NotificationMessageResolverTest {
  private final NotificationMessageResolver resolver = new NotificationMessageResolver();

  @Test
  void localizes_demand_message_in_all_supported_languages() {
    var demand =
        Demand.builder()
            .part(
                Part.builder()
                    .carBrand("Toyota")
                    .carModel("Corolla")
                    .name("Phare")
                    .carYear(Year.of(2018))
                    .build())
            .build();

    assertEquals(
        "Nouvelle demande : Toyota Corolla Phare (2018)",
        resolver.resolve(NotificationType.DEMAND_PUBLISHED, UserLanguage.FR, demand, "fallback"));
    assertEquals(
        "Fangatahana vaovao : Toyota Corolla Phare (2018)",
        resolver.resolve(NotificationType.DEMAND_PUBLISHED, UserLanguage.MG, demand, "fallback"));
    assertEquals(
        "New request: Toyota Corolla Phare (2018)",
        resolver.resolve(NotificationType.DEMAND_PUBLISHED, UserLanguage.EN, demand, "fallback"));
  }

  @Test
  void defaults_to_french_and_preserves_custom_system_announcements() {
    assertEquals(
        "Nouvelle offre reçue pour votre demande",
        resolver.resolve(NotificationType.OFFER_PUBLISHED, null, null, "fallback"));
    assertEquals(
        "Maintenance prévue",
        resolver.resolve(
            NotificationType.SYSTEM_ANNOUNCEMENT, UserLanguage.EN, null, "Maintenance prévue"));
  }
}
