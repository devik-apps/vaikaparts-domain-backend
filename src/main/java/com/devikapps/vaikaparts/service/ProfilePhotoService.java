package com.devikapps.vaikaparts.service;

import static java.lang.String.format;
import static org.owasp.encoder.Encode.forJava;

import com.devikapps.vaikaparts.file.BucketComponent;
import com.devikapps.vaikaparts.file.TempFileManager;
import com.devikapps.vaikaparts.mapper.ImageUrlMapper;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JUser;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfilePhotoService {

  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of(
          "image/jpeg",
          "image/jpg",
          "image/png",
          "image/gif",
          "image/webp",
          "image/bmp",
          "image/svg+xml");

  private final BucketComponent bucketComponent;
  private final UserRepository userRepository;
  private final TempFileManager tempFileManager;
  private final ImageUrlMapper profilePhotoUrlService;

  @Transactional
  public String uploadPhoto(JUser user, MultipartFile photo) {
    validatePhoto(photo);

    var oldPhotoKey = user.getProfileImgKey();
    File tempFile = null;

    try {
      tempFile = tempFileManager.createSecureTempFile("profile-photo-", getFileExtension(photo));
      photo.transferTo(tempFile);

      var photoKey = generatePhotoKey(user.getId());
      bucketComponent.upload(tempFile, photoKey);

      user.setProfileImgKey(photoKey);
      user.setUpdatedAt(LocalDateTime.now());
      userRepository.save(user);

      if (oldPhotoKey != null && !oldPhotoKey.isBlank()) deletePhotoFromBucket(oldPhotoKey);

      log.info("Profile photo uploaded for user: {}", forJava(user.getId()));

      return profilePhotoUrlService.getPresignedUrl(photoKey);

    } catch (IOException e) {
      log.error("Failed to save photo to temp file for user: {}", forJava(user.getId()), e);
      throw new IllegalStateException("Failed to process photo", e);
    } finally {
      if (tempFile != null) tempFileManager.deleteTempFile(tempFile);
    }
  }

  @Transactional
  public void deletePhoto(JUser user) {
    String photoKey = user.getProfileImgKey();

    if (photoKey == null || photoKey.isBlank()) {
      log.debug("No profile photo to delete for user: {}", forJava(user.getId()));
      return;
    }

    deletePhotoFromBucket(photoKey);

    user.setProfileImgKey(null);
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);

    log.info("Profile photo deleted for user: {}", forJava(user.getId()));
  }

  private void validatePhoto(MultipartFile photo) {
    if (photo.isEmpty()) {
      log.warn("Photo upload rejected: file is empty");
      throw new IllegalArgumentException("Photo file is empty");
    }

    if (photo.getSize() > MAX_FILE_SIZE) {
      log.warn("Photo upload rejected: size {} exceeds limit {}", photo.getSize(), MAX_FILE_SIZE);
      throw new IllegalArgumentException(
          format("Photo size exceeds limit of %d MB", MAX_FILE_SIZE / 1024 / 1024));
    }

    String contentType = photo.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
      log.warn("Photo upload rejected: invalid content type {}", forJava(contentType));
      throw new IllegalArgumentException(
          format(
              "Invalid photo format '%s'. Allowed formats: %s",
              contentType, ALLOWED_CONTENT_TYPES));
    }
  }

  private String getFileExtension(MultipartFile photo) {
    String originalFilename = photo.getOriginalFilename();
    if (originalFilename != null && originalFilename.contains("."))
      return originalFilename.substring(originalFilename.lastIndexOf("."));

    return ".jpg";
  }

  private String generatePhotoKey(String userId) {
    String timestamp = String.valueOf(System.currentTimeMillis());
    return format("profiles/%s/photo-%s.jpg", userId, timestamp);
  }

  private void deletePhotoFromBucket(String photoKey) {
    try {
      bucketComponent.delete(photoKey);
      log.debug("Deleted photo from bucket: {}", forJava(photoKey));
    } catch (Exception e) {
      log.error("Failed to delete photo from bucket: {}", forJava(photoKey), e);
      // Don't throw - key will still be removed from database
    }
  }
}
