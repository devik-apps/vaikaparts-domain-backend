package com.devikapps.vaikaparts.service.notification;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.UserLanguage;
import com.devikapps.vaikaparts.model.exchange.Demand;
import com.devikapps.vaikaparts.model.exchange.Exchange;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageResolver {

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
      case OFFER_PUBLISHED ->
          translate(
              resolvedLanguage,
              "Nouvelle offre reçue pour votre demande",
              "Tolotra vaovao voaray ho an'ny fangatahanao",
              "New offer received for your request");
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
      case SYSTEM_ANNOUNCEMENT -> fallbackMessage;
    };
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
