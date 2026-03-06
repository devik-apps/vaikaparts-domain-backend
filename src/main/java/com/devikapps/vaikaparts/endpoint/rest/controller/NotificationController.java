package com.devikapps.vaikaparts.endpoint.rest.controller;

import com.devikapps.vaikaparts.model.notification.DemandPublishedNotification;
import com.devikapps.vaikaparts.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
  private final NotificationService notificationService;

  @GetMapping("/demand-published")
  public Page<DemandPublishedNotification> fetchNotifications(
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size) {
    return notificationService.fetchAllNotification(page, size);
  }

  @GetMapping("/demand-published/{notificationId}")
  public DemandPublishedNotification getNotification(@PathVariable String notificationId) {
    return notificationService.getNotification(notificationId);
  }

  @PatchMapping("/demand-published/mark-as-read/{notificationId}")
  public ResponseEntity<DemandPublishedNotification> markAsRead(
      @PathVariable String notificationId) {
    return new ResponseEntity<>(
        notificationService.markAsRead(notificationId), HttpStatus.ACCEPTED);
  }
}
