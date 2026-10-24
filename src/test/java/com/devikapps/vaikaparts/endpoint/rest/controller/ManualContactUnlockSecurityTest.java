package com.devikapps.vaikaparts.endpoint.rest.controller;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.config.JwtAuthenticationFilter;
import com.devikapps.vaikaparts.config.sec.SecurityConf;
import com.devikapps.vaikaparts.endpoint.rest.controller.exchange.ManualContactUnlockController;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.ContactUnlockResponse;
import com.devikapps.vaikaparts.service.JwtValidationService;
import com.devikapps.vaikaparts.service.ManualContactUnlockService;
import com.devikapps.vaikaparts.service.UserCreationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ManualContactUnlockController.class)
@Import({SecurityConf.class, JwtAuthenticationFilter.class})
class ManualContactUnlockSecurityTest {

  @Autowired private MockMvc mvc;
  @MockitoBean private ManualContactUnlockService service;
  @MockitoBean private JwtValidationService jwtValidationService;
  @MockitoBean private UserCreationService userCreationService;

  @Test
  void shouldRejectResearcherConfirmation() throws Exception {
    mvc.perform(
            post("/v1/admin/contact-unlocks/unlock-id/confirm")
                .with(user("buyer").roles("RESEARCHER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldAllowManagerConfirmation() throws Exception {
    when(service.confirm(any(), any(), any()))
        .thenReturn(new ContactUnlockResponse("unlock-id", RELEASED, null));

    mvc.perform(
            post("/v1/admin/contact-unlocks/unlock-id/confirm")
                .with(user("manager").roles("MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk());
  }
}
