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
import java.util.Locale;
import java.util.Set;
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

  private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
  private static final long MAX_TOTAL_IMAGE_SIZE_BYTES = 25 * 1024 * 1024;
  private static final Set<String> SUPPORTED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");

  private final BucketComponent bucketComponent;
  private final TempFileManager tempFileManager;
  private final FilenameSanitizer filenameSanitizer;

  @Override
  public List<String> apply(List<MultipartFile> images, String bucketPrefix) {
    if (images == null || images.isEmpty()) {
      log.debug("No images to upload for bucket prefix: {}", forJava(bucketPrefix));
      return List.of();
    }

    List<MultipartFile> nonEmptyImages =
        images.stream().filter(image -> image != null && !image.isEmpty()).toList();
    if (nonEmptyImages.isEmpty()) return List.of();
    validateImages(nonEmptyImages);

    log.info(
        "Uploading {} image(s) to bucket prefix: {}",
        nonEmptyImages.size(),
        forJava(bucketPrefix));

    List<String> bucketKeys =
        nonEmptyImages.stream().map(image -> uploadSingleImage(image, bucketPrefix)).toList();

    log.info("Successfully uploaded {} image(s)", bucketKeys.size());
    return bucketKeys;
  }

  private void validateImages(List<MultipartFile> images) {
    long totalSize = images.stream().mapToLong(MultipartFile::getSize).sum();
    if (images.stream()
        .anyMatch(
            image ->
                image.getContentType() == null
                    || !SUPPORTED_CONTENT_TYPES.contains(
                        image.getContentType().toLowerCase(Locale.ROOT)))) {
      throw new IllegalArgumentException(
          "Images must be JPEG, PNG, WebP, HEIC, or HEIF");
    }
    if (images.stream().anyMatch(image -> image.getSize() > MAX_IMAGE_SIZE_BYTES)) {
      throw new IllegalArgumentException("Each image must be no larger than 5 MB");
    }
    if (totalSize > MAX_TOTAL_IMAGE_SIZE_BYTES) {
      throw new IllegalArgumentException("The total image size must not exceed 25 MB");
    }
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
      bucketComponent.upload(tempFile, bucketKey, image.getContentType());
      log.debug("Successfully uploaded image: {}", forJava(bucketKey));
      return bucketKey;
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to upload image to bucket: " + forJava(bucketKey), e);
    } finally {
      tempFileManager.deleteTempFile(tempFile);
    }
  }
}
