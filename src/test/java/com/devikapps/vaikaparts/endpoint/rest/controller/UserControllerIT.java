package com.devikapps.vaikaparts.endpoint.rest.controller;

import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.mapper.user.ValueObjectMapper;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.City;
import com.devikapps.vaikaparts.model.classifier.ManagerRole;
import com.devikapps.vaikaparts.model.classifier.Region;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.JLatLon;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.util.Collections;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

class UserControllerIT extends FacadeIT {

  private static final String BASE_URL = "/users/me";
  private static final String PHOTO_URL = BASE_URL + "/profile-photo";

  @Autowired private MockMvc mvc;
  @Autowired private UserRepository ur;
  @Autowired private ValueObjectMapper vom;

  private JResearcher testResearcher;
  private JSeller testSeller;
  private JManager testManager;

  @BeforeEach
  void setUp() {
    testResearcher = createResearcher();
    testSeller = createSeller();
    testManager = createManager();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    ur.deleteAll();
  }

  @Test
  void should_get_current_researcher_successfully() throws Exception {
    authenticateUser(testResearcher.getSupabaseUserId());

    mvc.perform(get(BASE_URL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testResearcher.getId()))
        .andExpect(jsonPath("$.name").value(testResearcher.getName()))
        .andExpect(jsonPath("$.phone_number").value(testResearcher.getPhoneNumber()))
        .andExpect(jsonPath("$.user_type").value("RESEARCHER"));
  }

  @Test
  void should_get_current_seller_successfully() throws Exception {
    authenticateUser(testSeller.getSupabaseUserId());

    mvc.perform(get(BASE_URL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testSeller.getId()))
        .andExpect(jsonPath("$.name").value(testSeller.getName()))
        .andExpect(jsonPath("$.garage_name").value(testSeller.getGarageName()))
        .andExpect(jsonPath("$.user_type").value("SELLER"));
  }

  @Test
  void should_get_current_manager_successfully() throws Exception {
    authenticateUser(testManager.getSupabaseUserId());

    mvc.perform(get(BASE_URL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(testManager.getId()))
        .andExpect(jsonPath("$.name").value(testManager.getName()))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.user_type").value("MANAGER"));
  }

  @Test
  void should_return_401_when_not_authenticated() throws Exception {
    SecurityContextHolder.clearContext();

    mvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());
  }

  @Test
  void should_upload_profile_photo_successfully() throws Exception {
    authenticateUser(testResearcher.getSupabaseUserId());

    val photo = createMockJpegPhoto();

    mvc.perform(multipart(PHOTO_URL).file(photo))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.photo_url").exists())
        .andExpect(jsonPath("$.photo_url").isString());

    val updatedUser = ur.findById(testResearcher.getId()).orElseThrow();
    assertNotNull(updatedUser.getProfileImgKey());
    assertTrue(updatedUser.getProfileImgKey().startsWith("profiles/" + testResearcher.getId()));
  }

  @Test
  void should_upload_png_photo_successfully() throws Exception {
    authenticateUser(testResearcher.getSupabaseUserId());

    val photo = createMockPngPhoto();

    mvc.perform(multipart(PHOTO_URL).file(photo))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.photo_url").exists());

    val updatedUser = ur.findById(testResearcher.getId()).orElseThrow();
    assertNotNull(updatedUser.getProfileImgKey());
  }

  @Test
  void should_reject_empty_photo() throws Exception {
    authenticateUser(testResearcher.getSupabaseUserId());

    val emptyPhoto = new MockMultipartFile("photo", "empty.jpg", "image/jpeg", new byte[0]);

    mvc.perform(multipart(PHOTO_URL).file(emptyPhoto)).andExpect(status().isBadRequest());
  }

  @Test
  void should_reject_photo_exceeding_size_limit() throws Exception {
    authenticateUser(testResearcher.getSupabaseUserId());

    byte[] largeContent = new byte[6 * 1024 * 1024]; // 6 MB
    val largePhoto = new MockMultipartFile("photo", "large.jpg", "image/jpeg", largeContent);

    mvc.perform(multipart(PHOTO_URL).file(largePhoto)).andExpect(status().isBadRequest());
  }

  @Test
  void should_reject_invalid_photo_format() throws Exception {
    authenticateUser(testResearcher.getSupabaseUserId());

    val invalidPhoto =
        new MockMultipartFile(
            "photo", "document.pdf", "application/pdf", "fake pdf content".getBytes());

    mvc.perform(multipart(PHOTO_URL).file(invalidPhoto)).andExpect(status().isBadRequest());
  }

  @Test
  void should_replace_old_photo_when_uploading_new_one() throws Exception {
    authenticateUser(testResearcher.getSupabaseUserId());

    val firstPhoto = createMockJpegPhoto();
    mvc.perform(multipart(PHOTO_URL).file(firstPhoto)).andExpect(status().isOk());

    val userAfterFirst = ur.findById(testResearcher.getId()).orElseThrow();
    val firstPhotoKey = userAfterFirst.getProfileImgKey();

    val secondPhoto = createMockPngPhoto();
    mvc.perform(multipart(PHOTO_URL).file(secondPhoto)).andExpect(status().isOk());

    val userAfterSecond = ur.findById(testResearcher.getId()).orElseThrow();
    val secondPhotoKey = userAfterSecond.getProfileImgKey();

    assertNotEquals(firstPhotoKey, secondPhotoKey);
    assertNotNull(secondPhotoKey);
  }

  @Test
  void should_delete_profile_photo_successfully() throws Exception {
    authenticateUser(testResearcher.getSupabaseUserId());

    val photo = createMockJpegPhoto();
    mvc.perform(multipart(PHOTO_URL).file(photo)).andExpect(status().isOk());

    val userBeforeDelete = ur.findById(testResearcher.getId()).orElseThrow();
    assertNotNull(userBeforeDelete.getProfileImgKey());

    mvc.perform(delete(PHOTO_URL)).andExpect(status().isNoContent());

    val userAfterDelete = ur.findById(testResearcher.getId()).orElseThrow();
    assertNull(userAfterDelete.getProfileImgKey());
  }

  @Test
  void should_not_fail_when_deleting_non_existent_photo() throws Exception {
    authenticateUser(testResearcher.getSupabaseUserId());

    mvc.perform(delete(PHOTO_URL)).andExpect(status().isNoContent());

    val user = ur.findById(testResearcher.getId()).orElseThrow();
    assertNull(user.getProfileImgKey());
  }

  @Test
  void should_return_401_when_uploading_photo_without_authentication() throws Exception {
    SecurityContextHolder.clearContext();

    val photo = createMockJpegPhoto();

    mvc.perform(multipart(PHOTO_URL).file(photo)).andExpect(status().isUnauthorized());
  }

  @Test
  void should_return_401_when_deleting_photo_without_authentication() throws Exception {
    SecurityContextHolder.clearContext();

    mvc.perform(delete(PHOTO_URL)).andExpect(status().isUnauthorized());
  }

  @Test
  void should_accept_webp_format() throws Exception {
    authenticateUser(testResearcher.getSupabaseUserId());

    val webpPhoto =
        new MockMultipartFile("photo", "image.webp", "image/webp", createMockImageBytes());

    mvc.perform(multipart(PHOTO_URL).file(webpPhoto))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.photo_url").exists());
  }

  @Test
  void should_accept_gif_format() throws Exception {
    authenticateUser(testResearcher.getSupabaseUserId());

    val gifPhoto = new MockMultipartFile("photo", "image.gif", "image/gif", createMockImageBytes());

    mvc.perform(multipart(PHOTO_URL).file(gifPhoto))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.photo_url").exists());
  }

  private JResearcher createResearcher() {
    var creationDate = now();
    val researcher =
        JResearcher.builder()
            .id(randomUUID().toString())
            .supabaseUserId("researcher-supabase-id")
            .name("Test Researcher")
            .phoneNumber("+1234567890")
            .profileImgKey(null)
            .location(vom.map(new Location(City.ANTANANARIVO, Region.ANALAMANGA, "Test Address")))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .createdAt(creationDate)
            .updatedAt(creationDate)
            .build();
    return ur.save(researcher);
  }

  private JSeller createSeller() {
    var creationDate = now();
    val seller =
        JSeller.builder()
            .id(randomUUID().toString())
            .supabaseUserId("seller-supabase-id")
            .name("Test Seller")
            .phoneNumber("+9876543210")
            .garageName("Test Garage")
            .profileImgKey(null)
            .location(vom.map(new Location(City.ANTANANARIVO, Region.ANALAMANGA, "Seller Address")))
            .latLon(new JLatLon(-18.8792, 47.5079))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .createdAt(creationDate)
            .updatedAt(creationDate)
            .build();
    return ur.save(seller);
  }

  private JManager createManager() {
    var creationDate = now();
    val manager =
        JManager.builder()
            .id(randomUUID().toString())
            .supabaseUserId("manager-supabase-id")
            .name("Test Manager")
            .phoneNumber("+1122334455")
            .profileImgKey(null)
            .managerRole(ManagerRole.ADMIN)
            .userType(UserType.MANAGER)
            .status(UserStatus.ENABLED)
            .createdAt(creationDate)
            .updatedAt(creationDate)
            .build();
    return ur.save(manager);
  }

  private void authenticateUser(String supabaseUserId) {
    val authentication =
        new UsernamePasswordAuthenticationToken(
            supabaseUserId,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(authentication);
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
      0x0A,
      0x00,
      0x00,
      0x00,
      0x0D,
      0x49,
      0x48,
      0x44,
      0x52,
      0x00,
      0x00,
      0x00,
      0x01,
      0x00,
      0x00,
      0x00,
      0x01,
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
      0x54,
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
      0x44,
      (byte) 0xAE,
      0x42,
      0x60,
      (byte) 0x82
    };
  }
}
