package com.devikapps.vaikaparts.endpoint.rest.controller.model;

import com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus;

public record ContactUnlockResponse(
    String unlockRequestId, ContactUnlockStatus status, String paymentUrl) {}
