package com.devikapps.vaikaparts.service;

import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.City;
import com.devikapps.vaikaparts.model.classifier.Region;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

class ProfilePhotoServiceIT extends FacadeIT {

  @Autowired private ProfilePhotoService profilePhotoService;
  @Autowired private UserRepository userRepository;
  @Autowired private ValueObjectMapper vom;

  private JResearcher testUser;

  @BeforeEach
  void setUp() {
    testUser =
        JResearcher.builder()
            .id(randomUUID().toString())
            .supabaseUserId("test-supabase-id-" + randomUUID())
            .name("Test User")
            .phoneNumber("+1234567890")
            .profileImgKey(null)
            .location(vom.map(new Location(City.ANTANANARIVO, Region.ANALAMANGA, "Test Address")))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .createdAt(now())
            .updatedAt(now())
            .build();

    userRepository.save(testUser);
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  void should_upload_profile_photo_successfully() {
    val photo = createMockJpegPhoto();

    val photoUrl = profilePhotoService.uploadPhoto(testUser, photo);

    assertNotNull(photoUrl);
    assertTrue(photoUrl.toString().contains("X-Amz"));

    val updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
    assertNotNull(updatedUser.getUpdatedAt());
  }

  @Test
  void should_upload_png_photo_successfully() {
    val photo = createMockPngPhoto();

    val photoUrl = profilePhotoService.uploadPhoto(testUser, photo);

    assertNotNull(photoUrl);
    assertTrue(photoUrl.toString().contains("X-Amz"));
    assertTrue(photoUrl.toString().contains("profiles/" + testUser.getId()));
  }

  @Test
  void should_replace_old_photo_when_uploading_new_one() {
    val oldPhoto = createMockJpegPhoto();
    val oldPhotoUrl = profilePhotoService.uploadPhoto(testUser, oldPhoto);
    val oldSubjectUser = userRepository.findById(testUser.getId()).orElseThrow();
    var oldBucketKey = oldSubjectUser.getProfileImgKey();

    val newPhoto = createMockPngPhoto();
    val newPhotoUrl = profilePhotoService.uploadPhoto(testUser, newPhoto);
    val newSubjectUser = userRepository.findById(testUser.getId()).orElseThrow();
    var newBucketKey = newSubjectUser.getProfileImgKey();

    assertNotEquals(oldPhotoUrl, newPhotoUrl);
    assertNotEquals(oldBucketKey, newBucketKey);
  }

  @Test
  void should_throw_exception_when_photo_is_empty() {
    val emptyPhoto = new MockMultipartFile("photo", "empty.jpg", "image/jpeg", new byte[0]);

    val exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> profilePhotoService.uploadPhoto(testUser, emptyPhoto));

    assertTrue(exception.getMessage().contains("empty"));
  }

  @Test
  void should_throw_exception_when_photo_exceeds_size_limit() {
    byte[] largeContent = new byte[6 * 1024 * 1024]; // 6 MB
    val largePhoto = new MockMultipartFile("photo", "large.jpg", "image/jpeg", largeContent);

    val exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> profilePhotoService.uploadPhoto(testUser, largePhoto));

    assertTrue(exception.getMessage().contains("exceeds limit"));
  }

  @Test
  void should_throw_exception_when_photo_format_is_invalid() {
    val invalidPhoto =
        new MockMultipartFile(
            "photo", "document.pdf", "application/pdf", "fake pdf content".getBytes());

    val exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> profilePhotoService.uploadPhoto(testUser, invalidPhoto));

    assertTrue(exception.getMessage().contains("Invalid photo format"));
  }

  @Test
  void should_delete_profile_photo_successfully() {
    val photo = createMockJpegPhoto();
    val photoUrl = profilePhotoService.uploadPhoto(testUser, photo);

    assertNotNull(photoUrl);
    assertTrue(photoUrl.toString().contains("X-Amz"));
    profilePhotoService.deletePhoto(testUser);

    val updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
    assertNull(updatedUser.getProfileImgKey());
  }

  @Test
  void should_not_fail_when_deleting_non_existent_photo() {
    testUser.setProfileImgKey(null);
    userRepository.save(testUser);

    assertDoesNotThrow(() -> profilePhotoService.deletePhoto(testUser));

    val updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
    assertNull(updatedUser.getProfileImgKey());
  }

  @Test
  void should_accept_webp_format() {
    val webpPhoto =
        new MockMultipartFile("photo", "image.webp", "image/webp", createMockImageBytes());

    val photoUrl = profilePhotoService.uploadPhoto(testUser, webpPhoto);

    assertNotNull(photoUrl);
    assertTrue(photoUrl.toString().contains("profiles/" + testUser.getId()));
  }

  @Test
  void should_accept_gif_format() {
    val gifPhoto = new MockMultipartFile("photo", "image.gif", "image/gif", createMockImageBytes());

    val photoUrl = profilePhotoService.uploadPhoto(testUser, gifPhoto);

    assertNotNull(photoUrl);
    assertTrue(photoUrl.toString().contains("profiles/" + testUser.getId()));
  }

  private MockMultipartFile createMockJpegPhoto() {
    return new MockMultipartFile("photo", "test-photo.jpg", "image/jpeg", createMockImageBytes());
  }

  private MockMultipartFile createMockPngPhoto() {
    return new MockMultipartFile("photo", "test-photo.png", "image/png", createMockImageBytes());
  }

  private byte[] createMockImageBytes() {
    return new byte[] {
      (byte) 0x89,
      0x50,
      0x4E,
      0x47,
      0x0D,
      0x0A,
      0x1A,
      0x0A, // PNG signature
      0x00,
      0x00,
      0x00,
      0x0D,
      0x49,
      0x48,
      0x44,
      0x52, // IHDR chunk
      0x00,
      0x00,
      0x00,
      0x01,
      0x00,
      0x00,
      0x00,
      0x01, // width=1, height=1
      0x08,
      0x02,
      0x00,
      0x00,
      0x00,
      (byte) 0x90,
      0x77,
      0x53,
      (byte) 0xDE,
      0x00,
      0x00,
      0x00,
      0x0C,
      0x49,
      0x44,
      0x41,
      0x54, // IDAT chunk
      0x08,
      (byte) 0xD7,
      0x63,
      (byte) 0xF8,
      (byte) 0xCF,
      (byte) 0xC0,
      0x00,
      0x00,
      0x03,
      0x01,
      0x01,
      0x00,
      0x18,
      (byte) 0xDD,
      (byte) 0x8D,
      (byte) 0xB4,
      0x00,
      0x00,
      0x00,
      0x00,
      0x49,
      0x45,
      0x4E,
      0x44, // IEND chunk
      (byte) 0xAE,
      0x42,
      0x60,
      (byte) 0x82
    };
  }
}
