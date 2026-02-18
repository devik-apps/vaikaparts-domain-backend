package com.devikapps.vaikaparts.service.util;

import static java.util.UUID.randomUUID;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.InfraGenerated;
import com.devikapps.vaikaparts.file.BucketComponent;
import com.devikapps.vaikaparts.file.FilenameSanitizer;
import com.devikapps.vaikaparts.file.TempFileManager;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@InfraGenerated
@RequiredArgsConstructor
public class ImageUploader implements BiFunction<List<MultipartFile>, String, List<String>> {

  private final BucketComponent bucketComponent;
  private final TempFileManager tempFileManager;
  private final FilenameSanitizer filenameSanitizer;

  @Override
  public List<String> apply(List<MultipartFile> images, String bucketPrefix) {
    if (images == null || images.isEmpty()) {
      log.debug("No images to upload for bucket prefix: {}", forJava(bucketPrefix));
      return List.of();
    }

    log.info("Uploading {} image(s) to bucket prefix: {}", images.size(), forJava(bucketPrefix));

    List<String> bucketKeys =
        images.stream().map(image -> uploadSingleImage(image, bucketPrefix)).toList();

    log.info("Successfully uploaded {} image(s)", bucketKeys.size());
    return bucketKeys;
  }

  private String uploadSingleImage(MultipartFile image, String bucketPrefix) {
    String originalFilename = image.getOriginalFilename();
    String sanitizedFilename =
        filenameSanitizer.apply(
            StringUtils.hasText(originalFilename) ? originalFilename : "upload");
    String bucketKey = bucketPrefix + randomUUID() + "-" + sanitizedFilename;

    log.debug("Uploading image to bucket: key={}", forJava(bucketKey));

    File tempFile;
    try {
      tempFile = tempFileManager.createSecureTempFile("upload-", "-" + sanitizedFilename);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to create temporary file for upload", e);
    }

    try {
      image.transferTo(tempFile);
      bucketComponent.upload(tempFile, bucketKey);
      log.debug("Successfully uploaded image: {}", forJava(bucketKey));
      return bucketKey;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to upload image to bucket: " + forJava(bucketKey), e);
    } finally {
      tempFileManager.deleteTempFile(tempFile);
    }
  }
}
