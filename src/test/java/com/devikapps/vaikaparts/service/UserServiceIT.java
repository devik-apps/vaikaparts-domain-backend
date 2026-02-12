package com.devikapps.vaikaparts.service;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devikapps.vaikaparts.conf.FacadeIT;
import com.devikapps.vaikaparts.exception.UserNotFoundException;
import com.devikapps.vaikaparts.mapper.user.ValueObjectMapper;
import com.devikapps.vaikaparts.model.LatLon;
import com.devikapps.vaikaparts.model.Location;
import com.devikapps.vaikaparts.model.classifier.ManagerRole;
import com.devikapps.vaikaparts.model.classifier.UserStatus;
import com.devikapps.vaikaparts.model.classifier.UserType;
import com.devikapps.vaikaparts.repository.UserRepository;
import com.devikapps.vaikaparts.repository.model.user.JManager;
import com.devikapps.vaikaparts.repository.model.user.JResearcher;
import com.devikapps.vaikaparts.repository.model.user.JSeller;
import java.util.Collections;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

    assertNotNull(seller);
    assertInstanceOf(JSeller.class, seller);
    assertEquals(TEST_SUPABASE_USER_ID, seller.getSupabaseUserId());
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
            .location(vom.map(Location.getDefault()))
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
            .phoneNumber("+26122222222")
            .location(vom.map(Location.getDefault()))
            .latLon(vom.map(LatLon.getDefault()))
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
