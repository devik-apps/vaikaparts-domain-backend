package com.devikapps.vaikaparts.service.util;

import com.devikapps.vaikaparts.model.classifier.PostStatus;
import java.time.LocalDateTime;

public interface StatusAware {

  void setStatus(PostStatus status);

  void setUpdatedAt(LocalDateTime dateTime);

  void setCanceledAt(LocalDateTime dateTime);

  void setSuspendedAt(LocalDateTime dateTime);
}
