package com.devikapps.vaikaparts.service;

import static java.time.Duration.ofHours;

import com.devikapps.vaikaparts.file.BucketComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfilePhotoUrlService {

  private final BucketComponent bucketComponent;

  public String getPresignedUrl(String profileImgKey) {
    if (profileImgKey == null || profileImgKey.isBlank()) return null;

    var presignedUrl = bucketComponent.presign(profileImgKey, ofHours(1));
    return presignedUrl.toString();
  }
}
