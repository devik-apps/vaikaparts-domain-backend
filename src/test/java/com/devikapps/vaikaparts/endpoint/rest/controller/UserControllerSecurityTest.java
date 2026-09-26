package com.devikapps.vaikaparts.endpoint.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.config.JwtAuthenticationFilter;
import com.devikapps.vaikaparts.config.sec.SecurityConf;
import com.devikapps.vaikaparts.endpoint.rest.controller.user.UserController;
import com.devikapps.vaikaparts.model.classifier.UserLanguage;
import com.devikapps.vaikaparts.model.user.Researcher;
import com.devikapps.vaikaparts.service.JwtValidationService;
import com.devikapps.vaikaparts.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConf.class, JwtAuthenticationFilter.class})
class UserControllerSecurityTest {

  @Autowired private MockMvc mvc;
  @MockitoBean private UserService userService;
  @MockitoBean private JwtValidationService jwtValidationService;

  @Test
  void authenticated_patch_does_not_require_csrf_token() throws Exception {
    when(userService.updatePreferredLanguage(UserLanguage.MG))
        .thenReturn(Researcher.builder().preferredLanguage(UserLanguage.MG).build());

    mvc.perform(patch("/users/me/language").param("language", "MG").with(user("user-id")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.preferred_language").value("MG"));
  }

  @Test
  void unauthenticated_patch_remains_rejected() throws Exception {
    mvc.perform(patch("/users/me/language").param("language", "MG"))
        .andExpect(status().isUnauthorized());
  }
}
