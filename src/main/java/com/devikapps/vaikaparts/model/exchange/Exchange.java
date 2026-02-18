package com.devikapps.vaikaparts.model.exchange;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.classifier.PostStatus;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode
@Getter
@Setter
public abstract class Exchange {
  @NotNull private String id;
  private String description;
  private List<URL> attachedPhotosUrls;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime canceledAt;
  private LocalDateTime suspendedAt;
  private PostStatus status;

  @Override
  public String toString() {
    return format(
        """
        Exchange={
        \tid=%s,
        \tdescription=%s,
        \tattachedPhotos=%s,
        \tstatus=%s,
        \tcreatedAt=%s,
        \tupdatedAt=%s,
        \tsuspendedAt=%s,
        \tcanceledAt=%s
        }\
        """,
        id, description, attachedPhotosUrls, status, createdAt, updatedAt, suspendedAt, canceledAt);
  }
}
