package com.devikapps.vaikaparts.service.notification.push;

import com.devikapps.vaikaparts.repository.WebPushSubscriptionRepository;
import com.devikapps.vaikaparts.repository.model.notification.JWebPushSubscription;
import com.devikapps.vaikaparts.service.UserService;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebPushSubscriptionService {
  private final WebPushSubscriptionRepository repository;
  private final UserService users;
  private final WebPushEndpointValidator endpointValidator;

  @Transactional
  public JWebPushSubscription register(String endpoint, String p256dh, String auth) {
    endpointValidator.validate(endpoint);
    if (p256dh == null
        || p256dh.isBlank()
        || p256dh.length() > 512
        || auth == null
        || auth.isBlank()
        || auth.length() > 512) {
      throw new IllegalArgumentException(
          "Subscription keys are required and must be at most 512 characters");
    }
    var user = users.getCurrentUser();
    var now = OffsetDateTime.now();
    var subscription =
        repository
            .findByEndpoint(endpoint)
            .orElseGet(
                () ->
                    JWebPushSubscription.builder()
                        .id(UUID.randomUUID().toString())
                        .user(user)
                        .endpoint(endpoint)
                        .createdAt(now)
                        .build());
    if (!subscription.getUser().getId().equals(user.getId())) {
      throw new IllegalStateException("Subscription belongs to another user");
    }
    subscription.setP256dh(p256dh);
    subscription.setAuth(auth);
    subscription.setUpdatedAt(now);
    return repository.save(subscription);
  }

  @Transactional
  public void delete(String endpoint) {
    repository.deleteByEndpointAndUserId(endpoint, users.getCurrentUser().getId());
  }
}
