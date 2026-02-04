package com.devikapps.vaikaparts.endpoint.rest.controller.health;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devikapps.vaikaparts.InfraGenerated;
import com.devikapps.vaikaparts.conf.FacadeIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@InfraGenerated
class PingPongControllerIT extends FacadeIT {
  @Autowired MockMvc mvc;

  @Test
  void should_respond_by_pong() throws Exception {
    var expected = "pong";

    mvc.perform(get("/ping"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(expected)));
  }
}
