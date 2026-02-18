package com.devikapps.vaikaparts.model.exchange;

import static java.lang.String.format;

import com.devikapps.vaikaparts.model.user.Researcher;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public final class Demand extends Exchange {
  private Part part;
  private Researcher researcher;

  @Override
  public String toString() {
    return format(
        """
        Demand={
        \tid=%s,
        \tdescription=%s,
        \tattachedPhotos=%s,
        \tstatus=%s,
        \tcreatedAt=%s,
        \tupdatedAt=%s,
        \tsuspendedAt=%s,
        \tcanceledAt=%s,
        \t%s,
        \t%s
        }\
        """,
        getId(),
        getDescription(),
        getAttachedPhotosUrls(),
        getStatus(),
        getCreatedAt(),
        getUpdatedAt(),
        getSuspendedAt(),
        getCanceledAt(),
        researcher,
        part);
  }
}
