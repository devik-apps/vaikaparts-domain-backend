package com.devikapps.vaikaparts.service.event;

import static com.devikapps.vaikaparts.model.classifier.UserStatus.ENABLED;
import static com.devikapps.vaikaparts.model.classifier.UserType.SELLER;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.nameUUIDFromBytes;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.endpoint.rest.controller.model.NotificationRequest;
import com.devikapps.vaikaparts.event.model.DemandUpdatedRequested;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.model.classifier.NotificationType;
import com.devikapps.vaikaparts.repository.DemandRepository;
import com.devikapps.vaikaparts.repository.OfferRepository;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import java.util.LinkedHashSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandUpdatedRequestedService implements Consumer<DemandUpdatedRequested> {

  private final DemandRepository demandRepository;
  private final OfferRepository offerRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  @Override
  @Transactional
  public void accept(DemandUpdatedRequested event) {
    var demand =
        demandRepository
            .findByIdWithRelations(event.getDemandId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        format("Demand %s not found", forJava(event.getDemandId()))));
    if (demand.getPart() == null) throw new IllegalStateException("Demand has no part");

    var activeSellers =
        userRepository.findAllByUserTypeAndStatus(SELLER, ENABLED).stream()
            .map(JSeller.class::cast)
            .toList();
    var activeSellerIds =
        activeSellers.stream().map(JSeller::getId).collect(Collectors.toSet());
    var recipients = new LinkedHashSet<String>();
    activeSellers.stream()
        .filter(
            seller ->
                Boolean.TRUE.equals(seller.getHandleAllCategory())
                    || (demand.getPart().getPartCategory() != null
                        && seller.getCategoryList() != null
                        && seller.getCategoryList().contains(demand.getPart().getPartCategory())))
        .map(JSeller::getId)
        .forEach(recipients::add);
    offerRepository.findDistinctSellerIdsByDemandId(event.getDemandId()).stream()
        .filter(activeSellerIds::contains)
        .forEach(recipients::add);

    for (var sellerId : recipients) {
      var notificationId =
          nameUUIDFromBytes((event.getId() + ":" + sellerId).getBytes(UTF_8)).toString();
      notificationService.createAndSendNotification(
          NotificationRequest.builder()
              .recipientUserId(sellerId)
              .resourceId(event.getDemandId())
              .notificationType(NotificationType.DEMAND_UPDATED)
              .message("Une demande de pièce a été modifiée.")
              .clickAction(
                  format("{\"action\":\"VIEW_DEMAND\",\"demandId\":\"%s\"}", demand.getId()))
              .notificationId(notificationId)
              .build());
    }

    log.info(
        "Demand update notifications prepared: eventId={}, demandId={}, recipients={}",
        forJava(event.getId()),
        forJava(event.getDemandId()),
        recipients.size());
  }
}
