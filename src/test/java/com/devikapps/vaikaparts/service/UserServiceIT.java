package com.devikapps.vaikaparts.service;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.exception.UserNotFoundException;
import com.devikapps.vaikaparts.mapper.ValueObjectMapper;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class UserServiceIT extends FacadeIT {

  private static final String TEST_SUPABASE_USER_ID = "supabase-user-123";
  private static final String TEST_USER_ID = "user-123";
  private static final String TEST_NAME = "John Doe";

  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private ValueObjectMapper vom;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    userRepository.deleteAll();
  }

  @Test
  void should_return_current_user_when_authenticated_researcher() {
    createAndAuthenticateResearcher();

    val currentUser = userService.getCurrentUser();

    assertNotNull(currentUser);
    assertEquals(TEST_SUPABASE_USER_ID, currentUser.getSupabaseUserId());
    assertEquals(TEST_USER_ID, currentUser.getId());
    assertEquals(TEST_NAME, currentUser.getName());
    assertEquals(UserType.RESEARCHER, currentUser.getUserType());
  }

  @Test
  void should_return_current_researcher_when_authenticated_as_researcher() {
    createAndAuthenticateResearcher();

    val researcher = userService.getCurrentResearcher();

    assertNotNull(researcher);
    assertInstanceOf(JResearcher.class, researcher);
    assertEquals(TEST_SUPABASE_USER_ID, researcher.getSupabaseUserId());
  }

  @Test
  void should_return_current_seller_when_authenticated_as_seller() {
    createAndAuthenticateSeller();

    val seller = userService.getCurrentSeller();
    val expectedLatLon = new JLatLon(1.1, 2.2);

    assertNotNull(seller);
    assertInstanceOf(JSeller.class, seller);
    assertEquals(TEST_SUPABASE_USER_ID, seller.getSupabaseUserId());
    assertEquals("Seller garage", seller.getGarageName());
    assertEquals("Seller Name", seller.getName());
    assertEquals("+260330012332", seller.getPhoneNumber());
    assertEquals(expectedLatLon.toString(), seller.getLatLon().toString());
    assertEquals("Adiresy", seller.getLocation().getAddress());
  }

  @Test
  void should_return_current_manager_when_authenticated_as_manager() {
    createAndAuthenticateManager();

    val manager = userService.getCurrentManager();

    assertNotNull(manager);
    assertInstanceOf(JManager.class, manager);
    assertEquals(TEST_SUPABASE_USER_ID, manager.getSupabaseUserId());
  }

  @Test
  void should_throw_exception_when_no_authentication_present() {
    SecurityContextHolder.clearContext();

    assertThrows(
        AuthenticationCredentialsNotFoundException.class, () -> userService.getCurrentUser());
  }

  @Test
  void should_throw_exception_when_user_not_found_in_database() {
    setAuthenticatedUser("non-existent-supabase-id");

    assertThrows(UserNotFoundException.class, () -> userService.getCurrentUser());
  }

  @Test
  void should_throw_exception_when_requesting_researcher_but_user_is_seller() {
    createAndAuthenticateSeller();

    val exception =
        assertThrows(IllegalStateException.class, () -> userService.getCurrentResearcher());

    assertTrue(exception.getMessage().contains("User is not a RESEARCHER"));
  }

  @Test
  void should_throw_exception_when_requesting_seller_but_user_is_researcher() {
    createAndAuthenticateResearcher();

    val exception = assertThrows(IllegalStateException.class, () -> userService.getCurrentSeller());

    assertTrue(exception.getMessage().contains("User is not a SELLER"));
  }

  @Test
  void should_throw_exception_when_requesting_manager_but_user_is_researcher() {
    createAndAuthenticateResearcher();

    val exception =
        assertThrows(IllegalStateException.class, () -> userService.getCurrentManager());

    assertTrue(exception.getMessage().contains("User is not a MANAGER"));
  }

  @Test
  void should_return_user_by_supabase_id() {
    createResearcher();

    val user = userService.getUserBySupabaseId(TEST_SUPABASE_USER_ID);

    assertNotNull(user);
    assertEquals(TEST_SUPABASE_USER_ID, user.getSupabaseUserId());
    assertEquals(TEST_USER_ID, user.getId());
  }

  @Test
  void should_throw_exception_when_getting_user_by_non_existent_supabase_id() {
    assertThrows(
        UserNotFoundException.class, () -> userService.getUserBySupabaseId("non-existent-id"));
  }

  @Test
  void should_handle_multiple_calls_consistently() {
    createAndAuthenticateResearcher();

    val user1 = userService.getCurrentUser();
    val user2 = userService.getCurrentUser();
    val researcher = userService.getCurrentResearcher();

    assertEquals(user1.getId(), user2.getId());
    assertEquals(user1.getId(), researcher.getId());
  }

  @Test
  void should_work_with_different_authenticated_users() {
    val researcher = createResearcher();
    setAuthenticatedUser(researcher.getSupabaseUserId());

    val currentUser1 = userService.getCurrentUser();
    assertEquals(researcher.getId(), currentUser1.getId());

    SecurityContextHolder.clearContext();

    val seller = createSeller("seller-supabase-id", "seller-id");
    setAuthenticatedUser(seller.getSupabaseUserId());

    val currentUser2 = userService.getCurrentUser();
    assertEquals(seller.getId(), currentUser2.getId());
    assertNotEquals(currentUser1.getId(), currentUser2.getId());
  }

  @Test
  void should_upload_profile_photo_successfully() {
    createAndAuthenticateResearcher();

    val photo = new MockMultipartFile("photo", "test.jpg", "image/jpeg", createMockImageBytes());

    val photoUrl = userService.uploadProfilePhoto(photo);

    assertNotNull(photoUrl);
    assertTrue(photoUrl.toString().contains("X-Amz"));
  }

  @Test
  void should_delete_profile_photo_successfully() {
    createAndAuthenticateResearcher();

    val photo = new MockMultipartFile("photo", "test.jpg", "image/jpeg", createMockImageBytes());
    userService.uploadProfilePhoto(photo);

    val userBeforeDelete = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID).orElseThrow();
    assertNotNull(userBeforeDelete.getProfileImgKey());

    userService.deleteProfilePhoto();

    val userAfterDelete = userRepository.findBySupabaseUserId(TEST_SUPABASE_USER_ID).orElseThrow();
    assertNull(userAfterDelete.getProfileImgKey());
  }

  @Test
  void should_throw_exception_when_uploading_invalid_photo() {
    createAndAuthenticateResearcher();

    val invalidPhoto =
        new MockMultipartFile("photo", "doc.pdf", "application/pdf", "pdf content".getBytes());

    assertThrows(
        IllegalArgumentException.class, () -> userService.uploadProfilePhoto(invalidPhoto));
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

  private void createAndAuthenticateResearcher() {
    val researcher = createResearcher();
    setAuthenticatedUser(researcher.getSupabaseUserId());
  }

  private void createAndAuthenticateSeller() {
    val seller = createSeller(TEST_SUPABASE_USER_ID, TEST_USER_ID);
    setAuthenticatedUser(seller.getSupabaseUserId());
  }

  private void createAndAuthenticateManager() {
    val manager = createManager();
    setAuthenticatedUser(manager.getSupabaseUserId());
  }

  private JResearcher createResearcher() {
    val researcher =
        JResearcher.builder()
            .id(TEST_USER_ID)
            .supabaseUserId(TEST_SUPABASE_USER_ID)
            .name(TEST_NAME)
            .phoneNumber("+26122222222")
            .location(vom.map(new Location(City.ANTANANARIVO, Region.ANALAMANGA, "Tesita")))
            .userType(UserType.RESEARCHER)
            .status(UserStatus.ENABLED)
            .createdAt(now())
            .updatedAt(now())
            .build();
    return userRepository.save(researcher);
  }

  private JSeller createSeller(String supabaseUserId, String userId) {
    val seller =
        JSeller.builder()
            .id(userId)
            .supabaseUserId(supabaseUserId)
            .garageName("Seller garage")
            .name("Seller Name")
            .phoneNumber("+260330012332")
            .location(vom.map(new Location(City.ANTANANARIVO, Region.ANALAMANGA, "Adiresy")))
            .latLon(new JLatLon(1.1, 2.2))
            .userType(UserType.SELLER)
            .status(UserStatus.ENABLED)
            .createdAt(now())
            .updatedAt(now())
            .build();
    return userRepository.save(seller);
  }

  private JManager createManager() {
    val manager =
        JManager.builder()
            .id(TEST_USER_ID)
            .supabaseUserId(TEST_SUPABASE_USER_ID)
            .name(TEST_NAME)
            .userType(UserType.MANAGER)
            .status(UserStatus.ENABLED)
            .managerRole(ManagerRole.ADMIN)
            .phoneNumber("+26122222222")
            .createdAt(now())
            .updatedAt(now())
            .build();
    return userRepository.save(manager);
  }

  private void setAuthenticatedUser(String supabaseUserId) {
    val authentication =
        new UsernamePasswordAuthenticationToken(
            supabaseUserId,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
