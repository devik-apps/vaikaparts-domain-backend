package com.devikapps.vaikaparts.service.notification;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.UserLanguage;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Exchange;
import com.devikapps.vaikaparts.model.exchange.Offer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageResolver {

  private static final String DEFAULT_FRONTEND_BASE_URL = "https://vaikaparts.com";
  private final String frontendBaseUrl;

  public NotificationMessageResolver() {
    this(DEFAULT_FRONTEND_BASE_URL);
  }

  @Autowired
  public NotificationMessageResolver(
      @Value("${vaikaparts.frontend-base-url:https://vaikaparts.com}") String frontendBaseUrl) {
    var configuredUrl =
        frontendBaseUrl == null || frontendBaseUrl.isBlank()
            ? DEFAULT_FRONTEND_BASE_URL
            : frontendBaseUrl.trim();
    this.frontendBaseUrl = configuredUrl.replaceFirst("/+$", "");
  }

  public String resolve(
      NotificationType type, UserLanguage language, Exchange resource, String fallbackMessage) {
    var resolvedLanguage = language == null ? UserLanguage.FR : language;
    if (type == NotificationType.SYSTEM_ANNOUNCEMENT) return fallbackMessage;

    return switch (type) {
      case DEMAND_PUBLISHED -> demandPublished(resolvedLanguage, resource, fallbackMessage);
      case DEMAND_CANCELED ->
          translate(
              resolvedLanguage,
              "Votre demande a été annulée",
              "Nofoanana ny fangatahanao",
              "Your request has been canceled");
      case OFFER_PUBLISHED -> offerPublished(resolvedLanguage, resource);
      case OFFER_ACCEPTED ->
          translate(
              resolvedLanguage,
              "Votre offre a été acceptée",
              "Nekena ny tolotrao",
              "Your offer has been accepted");
      case OFFER_REJECTED ->
          translate(
              resolvedLanguage,
              "Votre offre a été refusée",
              "Nolavina ny tolotrao",
              "Your offer has been rejected");
      case CONTACT_UNLOCKED -> fallbackMessage;
      case SYSTEM_ANNOUNCEMENT -> fallbackMessage;
    };
  }

  private String offerPublished(UserLanguage language, Exchange resource) {
    var message =
        translate(
            language,
            "Nouvelle offre reçue pour votre demande",
            "Tolotra vaovao voaray ho an'ny fangatahanao",
            "New offer received for your request");
    if (!(resource instanceof Offer offer) || offer.getId() == null || offer.getId().isBlank()) {
      return message;
    }
    return format("%s.%n%s/o/%s", message, frontendBaseUrl, offer.getId());
  }

  private String demandPublished(UserLanguage language, Exchange resource, String fallbackMessage) {
    if (!(resource instanceof Demand demand) || demand.getPart() == null) return fallbackMessage;

    var part = demand.getPart();
    var template =
        translate(
            language,
            "Nouvelle demande : %s %s %s (%s)",
            "Fangatahana vaovao : %s %s %s (%s)",
            "New request: %s %s %s (%s)");
    return format(
        template, part.getCarBrand(), part.getCarModel(), part.getName(), part.getCarYear());
  }

  private String translate(UserLanguage language, String french, String malagasy, String english) {
    return switch (language) {
      case FR -> french;
      case MG -> malagasy;
      case EN -> english;
    };
  }
}
