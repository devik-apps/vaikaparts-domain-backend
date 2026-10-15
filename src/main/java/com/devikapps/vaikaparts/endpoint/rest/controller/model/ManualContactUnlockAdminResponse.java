package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus;
import java.time.OffsetDateTime;

public record ManualContactUnlockAdminResponse(
    String unlockRequestId,
    String offerId,
    String buyerId,
    String buyerName,
    String buyerPhoneNumber,
    String sellerId,
    ContactUnlockStatus status,
    OffsetDateTime createdAt) {}
