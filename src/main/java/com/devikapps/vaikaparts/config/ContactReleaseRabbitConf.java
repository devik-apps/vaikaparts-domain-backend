package com.devikapps.vaikaparts.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContactReleaseRabbitConf {

  public static final String EXCHANGE = "vaikaparts.events";
  public static final String ROUTING_KEY = "payment.contact-release.requested";
  public static final String QUEUE = "domain.contact-release.requested";
  public static final String DEAD_LETTER_EXCHANGE = "vaikaparts.events.dlx";
  public static final String DEAD_LETTER_ROUTING_KEY =
      "payment.contact-release.requested.dead";
  public static final String DEAD_LETTER_QUEUE = "domain.contact-release.requested.dlq";

  @Value("${contact-release.rabbit.retry.max-attempts:3}")
  private int maxAttempts = 3;

  @Value("${contact-release.rabbit.retry.initial-interval:1000}")
  private long initialInterval = 1000;

  @Value("${contact-release.rabbit.retry.multiplier:2.0}")
  private double multiplier = 2.0;

  @Value("${contact-release.rabbit.retry.max-interval:10000}")
  private long maxInterval = 10000;

  @Bean
  public DirectExchange contactReleaseExchange() {
    return new DirectExchange(EXCHANGE, true, false);
  }

  @Bean
  public DirectExchange contactReleaseDeadLetterExchange() {
    return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
  }

  @Bean
  public Queue contactReleaseQueue() {
    return QueueBuilder.durable(QUEUE)
        .deadLetterExchange(DEAD_LETTER_EXCHANGE)
        .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
        .build();
  }

  @Bean
  public Queue contactReleaseDeadLetterQueue() {
    return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
  }

  @Bean
  public Binding contactReleaseBinding(
      Queue contactReleaseQueue, DirectExchange contactReleaseExchange) {
    return BindingBuilder.bind(contactReleaseQueue)
        .to(contactReleaseExchange)
        .with(ROUTING_KEY);
  }

  @Bean
  public Binding contactReleaseDeadLetterBinding(
      Queue contactReleaseDeadLetterQueue,
      DirectExchange contactReleaseDeadLetterExchange) {
    return BindingBuilder.bind(contactReleaseDeadLetterQueue)
        .to(contactReleaseDeadLetterExchange)
        .with(DEAD_LETTER_ROUTING_KEY);
  }

  @Bean(name = "contactReleaseListenerContainerFactory")
  public SimpleRabbitListenerContainerFactory contactReleaseListenerContainerFactory(
      ConnectionFactory connectionFactory) {
    var factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setDefaultRequeueRejected(false);

    Advice retryAdvice =
        RetryInterceptorBuilder.stateless()
            .maxAttempts(maxAttempts)
            .backOffOptions(initialInterval, multiplier, maxInterval)
            .recoverer(new RejectAndDontRequeueRecoverer())
            .build();
    factory.setAdviceChain(retryAdvice);
    return factory;
  }
}
