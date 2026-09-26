package com.devikapps.vaikaparts.service.notification;

import static org.owasp.encoder.Encode.forHtml;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.exception.NotificationDeliveryException;
import com.devikapps.vaikaparts.mail.Email;
import com.devikapps.vaikaparts.mail.Mailer;
import com.devikapps.vaikaparts.model.classifier.NotificationChannelType;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.model.classifier.UserLanguage;
import com.devikapps.vaikaparts.model.notification.Notification;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Enabled globally through NotificationChannelsConf; user preferences are checked by the service.
 */
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel {
  private final Mailer mailer;
  private final boolean enabled;

  @Override
  public void send(Notification notification) {
    if (!enabled) throw new IllegalStateException("Email notification channel is disabled");
    if (notification == null
        || notification.getRecipient() == null
        || notification.getNotificationType() == null
        || notification.getMessage() == null
        || notification.getMessage().isBlank()) {
      throw new IllegalArgumentException("A recipient, notification type and message are required");
    }
    var emailAddress = notification.getRecipient().getEmail();
    if (emailAddress == null
        || emailAddress.isBlank()
        || emailAddress.contains("\r")
        || emailAddress.contains("\n")) {
      throw new NotificationDeliveryException("A valid recipient email is required");
    }
    final InternetAddress recipient;
    try {
      recipient = new InternetAddress(emailAddress, true);
      recipient.validate();
    } catch (AddressException e) {
      throw new NotificationDeliveryException("A valid recipient email is required");
    }
    var subject =
        subject(
            notification.getNotificationType(), notification.getRecipient().getPreferredLanguage());
    var html =
        "<h1>"
            + forHtml(subject)
            + "</h1><p>"
            + forHtml(notification.getMessage()).replace("\n", "<br>")
            + "</p>";
    mailer.sendOrThrow(new Email(recipient, List.of(), List.of(), subject, html, List.of()));
    log.info(
        "[NOTIF-PIPELINE][EMAIL_ACCEPTED] notificationId={}, recipientType={} (not delivery"
            + " confirmation)",
        forJava(notification.getId()),
        notification.getRecipient().getUserType());
  }

  private String subject(NotificationType type, UserLanguage language) {
    var resolvedLanguage = language == null ? UserLanguage.FR : language;
    var localizedSubject =
        switch (resolvedLanguage) {
          case FR ->
              switch (type) {
                case DEMAND_PUBLISHED -> "Nouvelle demande de pièce";
                case DEMAND_CANCELED -> "Demande annulée";
                case OFFER_PUBLISHED -> "Nouvelle offre reçue";
                case OFFER_ACCEPTED -> "Offre acceptée";
                case OFFER_REJECTED -> "Offre refusée";
                case SYSTEM_ANNOUNCEMENT -> "Information";
              };
          case MG ->
              switch (type) {
                case DEMAND_PUBLISHED -> "Fangatahana kojakoja vaovao";
                case DEMAND_CANCELED -> "Nofoanana ny fangatahana";
                case OFFER_PUBLISHED -> "Tolotra vaovao voaray";
                case OFFER_ACCEPTED -> "Nekena ny tolotra";
                case OFFER_REJECTED -> "Nolavina ny tolotra";
                case SYSTEM_ANNOUNCEMENT -> "Fampahafantarana";
              };
          case EN ->
              switch (type) {
                case DEMAND_PUBLISHED -> "New part request";
                case DEMAND_CANCELED -> "Request canceled";
                case OFFER_PUBLISHED -> "New offer received";
                case OFFER_ACCEPTED -> "Offer accepted";
                case OFFER_REJECTED -> "Offer rejected";
                case SYSTEM_ANNOUNCEMENT -> "Information";
              };
        };
    return "VAIKAPARTS — " + localizedSubject;
  }

  @Override
  public NotificationChannelType getChannelType() {
    return NotificationChannelType.EMAIL;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
