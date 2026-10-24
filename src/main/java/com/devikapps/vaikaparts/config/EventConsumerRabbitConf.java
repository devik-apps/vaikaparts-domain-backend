package com.devikapps.vaikaparts.config;

import java.util.Map;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;

@Configuration
public class EventConsumerRabbitConf {

  @Value("${event-consumer.rabbit.retry.max-attempts:3}")
  private int maxAttempts = 3;

  @Value("${event-consumer.rabbit.retry.initial-interval:1000}")
  private long initialInterval = 1000;

  @Value("${event-consumer.rabbit.retry.multiplier:2.0}")
  private double multiplier = 2.0;

  @Value("${event-consumer.rabbit.retry.max-interval:10000}")
  private long maxInterval = 10000;

  @Bean(name = "eventConsumerRetryAdvice")
  public Advice eventConsumerRetryAdvice() {
    var retryableExceptions =
        Map.<Class<? extends Throwable>, Boolean>of(AmqpRejectAndDontRequeueException.class, false);
    var retryPolicy = new SimpleRetryPolicy(maxAttempts, retryableExceptions, true, true);

    return RetryInterceptorBuilder.stateless()
        .retryPolicy(retryPolicy)
        .backOffOptions(initialInterval, multiplier, maxInterval)
        .recoverer(
            (message, cause) -> {
              Throwable current = cause;
              while (current != null) {
                if (current instanceof AmqpRejectAndDontRequeueException rejection) {
                  throw rejection;
                }
                current = current.getCause();
              }
              throw new ImmediateRequeueAmqpException(
                  "Infrastructure event processing failed after retries", cause);
            })
        .build();
  }

  @Bean(name = "eventListenerContainerFactory")
  public SimpleRabbitListenerContainerFactory eventListenerContainerFactory(
      ConnectionFactory connectionFactory,
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      @Qualifier("eventConsumerRetryAdvice") Advice retryAdvice) {
    var factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setAdviceChain(retryAdvice);
    return factory;
  }
}
