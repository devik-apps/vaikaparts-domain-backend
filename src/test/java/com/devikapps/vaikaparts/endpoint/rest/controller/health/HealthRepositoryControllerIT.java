package com.devikapps.vaikaparts.endpoint.rest.controller.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.InfraGenerated;
import com.devikapps.vaikaparts.conf.FacadeIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@InfraGenerated
class HealthRepositoryControllerIT extends FacadeIT {

  @Autowired private MockMvc mvc;

  @Test
  void should_return_dummy_page() throws Exception {
    mvc.perform(get("/health/db").param("page", "0").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").exists())
        .andExpect(jsonPath("$.content[0].description").exists());
  }
}
