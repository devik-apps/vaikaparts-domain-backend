package com.devikapps.vaikaparts.model.exchange;

import static java.lang.String.format;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Offer extends Exchange {
  private String sellerId;
  private PartInfo partsInfo;
  private Demand demand;

  @Override
  public String toString() {
    return format(
        """
        Offer={
        \tid=%s,
        \tdescription=%s,
        \tattachedPhotos=%s,
        \tstatus=%s,
        \tcreatedAt=%s,
        \tupdatedAt=%s,
        \tsuspendedAt=%s,
        \tcanceledAt=%s,
        \tsellerId=%s,
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
        sellerId,
        partsInfo,
        demand);
  }
}
