package com.devikapps.vaikaparts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableScheduling
public class OutboxConfig {

  @Bean
  public TransactionTemplate outboxTransactionTemplate(
      PlatformTransactionManager transactionManager) {
    return new TransactionTemplate(transactionManager);
  }
}
