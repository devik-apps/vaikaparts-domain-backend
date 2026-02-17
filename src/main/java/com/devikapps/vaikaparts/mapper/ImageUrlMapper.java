package com.devikapps.vaikaparts.mapper;

import static java.time.Duration.ofDays;

import com.devikapps.vaikaparts.file.BucketComponent;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ImageUrlMapper {
  private final Duration DEFAULT_DURATION = ofDays(7L);
  private BucketComponent bucketComponent;

  @Autowired
  public void setBucketComponent(BucketComponent bucketComponent) {
    this.bucketComponent = bucketComponent;
  }

  @Named("getPresignedUrl")
  public URL getPresignedUrl(String profileImgKey) {
    if (profileImgKey == null || profileImgKey.isBlank()) return null;

    return bucketComponent.presign(profileImgKey, DEFAULT_DURATION);
  }

  @Named("toUrlList")
  public List<URL> toUrlList(List<String> bucketKeys) {
    if (bucketKeys == null || bucketKeys.isEmpty()) return new ArrayList<>();

    return bucketKeys.stream().map(b -> bucketComponent.presign(b, DEFAULT_DURATION)).toList();
  }
}
