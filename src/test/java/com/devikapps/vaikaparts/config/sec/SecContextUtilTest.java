package com.devikapps.vaikaparts.config.sec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecContextUtilTest {

  private static final String TEST_USER_ID = "supabase-user-123";

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_return_user_id_when_user_is_authenticated() {
    setAuthenticatedUser();

    var userId = SecContextUtil.getCurrentUserId();

    assertEquals(TEST_USER_ID, userId);
  }

  @Test
  void should_throw_exception_when_no_authentication_present() {
    SecurityContextHolder.clearContext();

    assertThrows(
        AuthenticationCredentialsNotFoundException.class, SecContextUtil::getCurrentUserId);
  }

  @Test
  void should_throw_exception_when_authentication_not_authenticated() {
    var authentication =
        new UsernamePasswordAuthenticationToken(TEST_USER_ID, null, Collections.emptyList());
    authentication.setAuthenticated(false);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertThrows(
        AuthenticationCredentialsNotFoundException.class, SecContextUtil::getCurrentUserId);
  }

  @Test
  void should_throw_exception_when_principal_is_not_string() {
    var principal = new Object();
    var authentication =
        new UsernamePasswordAuthenticationToken(
            principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    assertThrows(IllegalStateException.class, SecContextUtil::getCurrentUserId);
  }

  @Test
  void should_return_optional_with_user_id_when_authenticated() {
    setAuthenticatedUser();

    var userIdOpt = SecContextUtil.getCurrentUserIdOptional();

    assertTrue(userIdOpt.isPresent());
    assertEquals(TEST_USER_ID, userIdOpt.get());
  }

  @Test
  void should_return_empty_optional_when_no_authentication() {
    SecurityContextHolder.clearContext();

    var userIdOpt = SecContextUtil.getCurrentUserIdOptional();

    assertTrue(userIdOpt.isEmpty());
  }

  @Test
  void should_return_empty_optional_when_authentication_not_authenticated() {
    var authentication =
        new UsernamePasswordAuthenticationToken(TEST_USER_ID, null, Collections.emptyList());
    authentication.setAuthenticated(false);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    var userIdOpt = SecContextUtil.getCurrentUserIdOptional();

    assertTrue(userIdOpt.isEmpty());
  }

  @Test
  void should_return_empty_optional_when_principal_not_string() {
    var principal = new Object();
    var authentication =
        new UsernamePasswordAuthenticationToken(
            principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    var userIdOpt = SecContextUtil.getCurrentUserIdOptional();

    assertTrue(userIdOpt.isEmpty());
  }

  @Test
  void should_return_true_when_user_is_authenticated() {
    setAuthenticatedUser();

    var isAuth = SecContextUtil.isAuthenticated();

    assertTrue(isAuth);
  }

  @Test
  void should_return_false_when_no_authentication_present() {
    SecurityContextHolder.clearContext();

    var isAuth = SecContextUtil.isAuthenticated();

    assertFalse(isAuth);
  }

  @Test
  void should_return_false_when_authentication_not_authenticated() {
    var authentication =
        new UsernamePasswordAuthenticationToken(TEST_USER_ID, null, Collections.emptyList());
    authentication.setAuthenticated(false);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    var isAuth = SecContextUtil.isAuthenticated();

    assertFalse(isAuth);
  }

  @Test
  void should_return_false_when_principal_is_not_string() {
    var principal = new Object();
    var authentication =
        new UsernamePasswordAuthenticationToken(
            principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    var isAuth = SecContextUtil.isAuthenticated();

    assertFalse(isAuth);
  }

  @Test
  void should_handle_multiple_calls_consistently() {
    setAuthenticatedUser();

    var userId1 = SecContextUtil.getCurrentUserId();
    var userId2 = SecContextUtil.getCurrentUserId();
    var userIdOpt = SecContextUtil.getCurrentUserIdOptional();
    var isAuth = SecContextUtil.isAuthenticated();

    assertEquals(TEST_USER_ID, userId1);
    assertEquals(TEST_USER_ID, userId2);
    assertTrue(userIdOpt.isPresent());
    assertEquals(TEST_USER_ID, userIdOpt.get());
    assertTrue(isAuth);
  }

  @Test
  void should_throw_exception_when_trying_to_instantiate() {
    var exception =
        assertThrows(
            InvocationTargetException.class,
            () -> {
              var constructor = SecContextUtil.class.getDeclaredConstructor();
              constructor.setAccessible(true);
              constructor.newInstance();
            });

    assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
  }

  private void setAuthenticatedUser() {
    var authentication =
        new UsernamePasswordAuthenticationToken(
            SecContextUtilTest.TEST_USER_ID,
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
