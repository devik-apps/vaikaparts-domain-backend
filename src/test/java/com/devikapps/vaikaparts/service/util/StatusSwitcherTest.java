package com.devikapps.vaikaparts.service.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devikapps.vaikaparts.model.classifier.PostStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StatusSwitcherTest {

  @ParameterizedTest
  @EnumSource(
      value = PostStatus.class,
      names = {"DRAFT", "PENDING"})
  void should_reject_statuses_that_status_switcher_does_not_support(PostStatus status) {
    var entity = new TestStatusAware();

    assertThrows(IllegalArgumentException.class, () -> StatusSwitcher.updateStatus(entity, status));
  }

  @ParameterizedTest
  @EnumSource(
      value = PostStatus.class,
      names = {"CLOSED", "SATISFIED"})
  void should_accept_terminal_offer_outcomes(PostStatus status) {
    var entity = new TestStatusAware();

    assertDoesNotThrow(() -> StatusSwitcher.updateStatus(entity, status));

    assertEquals(status, entity.status);
  }

  private static final class TestStatusAware implements StatusAware {
    private PostStatus status;

    @Override
    public void setStatus(PostStatus status) {
      this.status = status;
    }

    @Override
    public void setUpdatedAt(LocalDateTime dateTime) {}

    @Override
    public void setCanceledAt(LocalDateTime dateTime) {}

    @Override
    public void setSuspendedAt(LocalDateTime dateTime) {}
  }
}
