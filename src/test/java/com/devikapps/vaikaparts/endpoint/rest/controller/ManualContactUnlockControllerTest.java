package com.devikapps.vaikaparts.endpoint.rest.controller;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.RELEASED;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.config.JacksonConf;
import com.devikapps.vaikaparts.endpoint.rest.controller.exchange.ManualContactUnlockController;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.ContactUnlockResponse;
import com.devikapps.vaikaparts.service.ManualContactUnlockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ManualContactUnlockControllerTest {

  @Mock private ManualContactUnlockService service;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new ManualContactUnlockController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .setMessageConverters(
                new MappingJackson2HttpMessageConverter(new JacksonConf().objectMapper()))
            .build();
  }

  @Test
  void shouldConfirmManualPayment() throws Exception {
    when(service.confirm("unlock-id", "TX-1", "Validé visuellement"))
        .thenReturn(new ContactUnlockResponse("unlock-id", RELEASED, null));

    mvc.perform(
            post("/v1/admin/contact-unlocks/unlock-id/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"payment_reference\":\"TX-1\",\"review_note\":\"Validé visuellement\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unlock_request_id").value("unlock-id"))
        .andExpect(jsonPath("$.status").value("RELEASED"));

    verify(service).confirm("unlock-id", "TX-1", "Validé visuellement");
  }
}
