package com.devikapps.vaikaparts.service.notification.push;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.repository.WebPushSubscriptionRepository;
import com.devikapps.vaikaparts.repository.model.notification.JWebPushSubscription;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import com.devikapps.vaikaparts.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebPushSubscriptionServiceTest {
  private final WebPushSubscriptionRepository repository =
      mock(WebPushSubscriptionRepository.class);
  private final UserService users = mock(UserService.class);
  private final WebPushSubscriptionService service =
      new WebPushSubscriptionService(repository, users, new WebPushEndpointValidator());
  private final String endpoint = "https://8.8.8.8/push/browser-1";

  @BeforeEach
  void setup() {
    when(users.getCurrentUser()).thenReturn(JUser.builder().id("domain-user").build());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void registerRejectsUnsafeEndpoint() {
    for (String unsafe :
        new String[] {
          "http://8.8.8.8/a", "https://user@8.8.8.8/a", "https://localhost/a",
          "https://127.0.0.1/a", "https://169.254.169.254/a", "https://10.0.0.1/a",
          "https://172.16.0.1/a", "https://192.168.1.1/a", "https://[::1]/a",
          "https://[fc00::1]/a", "https://127.1/a", "https://0.0.0.0/a",
          "https://localhost.localdomain/a", "file:///a", "https://8.8.8.8/a#fragment"
        }) {
      assertThrows(IllegalArgumentException.class, () -> service.register(unsafe, "key", "auth"));
    }
    verifyNoInteractions(repository, users);
  }

  @Test
  void registerUpsertsForCurrentUser() {
    var existing =
        JWebPushSubscription.builder()
            .id("existing")
            .user(JUser.builder().id("domain-user").build())
            .endpoint(endpoint)
            .p256dh("old")
            .auth("old")
            .build();
    when(repository.findByEndpoint(endpoint)).thenReturn(Optional.of(existing));
    service.register(endpoint, "new-key", "new-auth");
    assertEquals("domain-user", existing.getUser().getId());
    assertEquals("new-key", existing.getP256dh());
    assertEquals("new-auth", existing.getAuth());

    assertNotNull(existing.getUpdatedAt());
    verify(repository).save(existing);
  }

  @Test
  void registerSupportsMultipleEndpointsPerUser() {
    var first = service.register(endpoint, "key", "auth");
    var second = service.register("https://8.8.8.8/push/browser-2", "key2", "auth2");
    assertNotEquals(first.getId(), second.getId());
    assertEquals("domain-user", first.getUser().getId());
    assertEquals(first.getUser().getId(), second.getUser().getId());
    assertNotNull(first.getCreatedAt());
  }

  @Test
  void deleteIsIdempotentAndScopedToCurrentUser() {
    service.delete(endpoint);
    service.delete(endpoint);
    verify(repository, times(2)).deleteByEndpointAndUserId(endpoint, "domain-user");
    verify(repository, never()).deleteAll();
  }

  @Test
  void registerCannotOverwriteAnotherOwnersEndpoint() {
    var existing =
        JWebPushSubscription.builder()
            .user(JUser.builder().id("other-user").build())
            .p256dh("original")
            .build();
    when(repository.findByEndpoint(endpoint)).thenReturn(Optional.of(existing));
    assertThrows(IllegalStateException.class, () -> service.register(endpoint, "new", "auth"));
    assertEquals("original", existing.getP256dh());
    verify(repository, never()).save(any());
  }
}
