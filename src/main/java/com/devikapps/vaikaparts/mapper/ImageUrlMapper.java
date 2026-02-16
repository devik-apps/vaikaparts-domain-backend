package com.devikapps.vaikaparts.mapper;

import static java.time.Duration.ofHours;

import com.devikapps.vaikaparts.file.BucketComponent;
import java.time.Duration;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ImageUrlMapper {
  private final Duration DEFAULT_DURATION = ofHours(2L);
  private BucketComponent bucketComponent;

  @Autowired
  public void setBucketComponent(BucketComponent bucketComponent) {
    this.bucketComponent = bucketComponent;
  }

  @Named("getPresignedUrl")
  public String getPresignedUrl(String profileImgKey) {
    if (profileImgKey == null || profileImgKey.isBlank()) return null;

    var presignedUrl = bucketComponent.presign(profileImgKey, DEFAULT_DURATION);
    return presignedUrl.toString();
  }
}
