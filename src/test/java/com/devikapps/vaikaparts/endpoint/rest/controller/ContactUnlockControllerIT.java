package com.devikapps.vaikaparts.endpoint.rest.controller;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.endpoint.rest.controller.exchange.ContactUnlockController;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.ContactUnlockResponse;
import com.devikapps.vaikaparts.config.JacksonConf;
import com.devikapps.vaikaparts.service.ContactUnlockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@ExtendWith(MockitoExtension.class)
class ContactUnlockControllerIT {

  @Mock private ContactUnlockService service;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc =
        MockMvcBuilders.standaloneSetup(new ContactUnlockController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .setMessageConverters(
                new MappingJackson2HttpMessageConverter(new JacksonConf().objectMapper()))
            .build();
  }

  @Test
  void shouldInitiateVanillaPayContactUnlock() throws Exception {
    org.mockito.Mockito.when(service.initiate("offer-id", "VANILLA_PAY"))
        .thenReturn(
            new ContactUnlockResponse(
                "unlock-id", PENDING, "https://pay.example/checkout"));

    mvc.perform(
            post("/v1/offers/offer-id/contact-unlocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"VANILLA_PAY\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.unlock_request_id").value("unlock-id"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.payment_url").value("https://pay.example/checkout"));
  }

  @Test
  void shouldRejectMissingProvider() throws Exception {
    mvc.perform(
            post("/v1/offers/offer-id/contact-unlocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }
}
