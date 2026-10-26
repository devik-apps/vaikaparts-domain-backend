package com.devikapps.vaikaparts.service.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notifications.web-push")
@Getter
@Setter
public class WebPushProperties {
  private boolean enabled = false;
  private String publicKey = "";
  private String privateKey = "";
  private String subject = "";
}
