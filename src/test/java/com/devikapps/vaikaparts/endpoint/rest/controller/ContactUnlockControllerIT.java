package com.devikapps.vaikaparts.endpoint.rest.controller;

import static com.devikapps.vaikaparts.model.classifier.ContactUnlockStatus.PENDING;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.config.JacksonConf;
import com.devikapps.vaikaparts.endpoint.rest.controller.exchange.ContactUnlockController;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.ContactUnlockResponse;
import com.devikapps.vaikaparts.endpoint.rest.controller.model.SellerContactResponse;
import com.devikapps.vaikaparts.exception.ResourceNotFoundException;
import com.devikapps.vaikaparts.service.ContactUnlockService;
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

  @Test
  void shouldReturnCurrentSellerContactAfterRelease() throws Exception {
    org.mockito.Mockito.when(service.getSellerContact("offer-id"))
        .thenReturn(new SellerContactResponse("Seller Updated", "+261340000099", "new@example.com"));

    mvc.perform(get("/v1/offers/offer-id/contact-unlocks/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Seller Updated"))
        .andExpect(jsonPath("$.phone_number").value("+261340000099"))
        .andExpect(jsonPath("$.email").value("new@example.com"));
  }

  @Test
  void shouldNotExposeContactBeforeReleaseOrToAnotherBuyer() throws Exception {
    org.mockito.Mockito.when(service.getSellerContact("offer-id"))
        .thenThrow(new ResourceNotFoundException("Released contact unlock not found"));

    mvc.perform(get("/v1/offers/offer-id/contact-unlocks/me"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.phone_number").doesNotExist())
        .andExpect(jsonPath("$.email").doesNotExist());
  }
}
