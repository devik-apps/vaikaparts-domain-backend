package com.devikapps.vaikaparts.config;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.test.util.ReflectionTestUtils;

class EventConsumerRabbitConfTest {

  @Test
  void transient_failure_is_retried_a_bounded_number_of_times_then_requeued() throws Throwable {
    var configuration = configurationWithFastBackoff();
    var advice = (MethodInterceptor) configuration.eventConsumerRetryAdvice();
    var invocation = listenerInvocation();
    when(invocation.proceed()).thenThrow(new IllegalStateException("temporary failure"));

    assertThrows(ImmediateRequeueAmqpException.class, () -> advice.invoke(invocation));

    verify(invocation, times(3)).proceed();
  }

  @Test
  void malformed_message_is_rejected_immediately_without_retry() throws Throwable {
    var configuration = configurationWithFastBackoff();
    var advice = (MethodInterceptor) configuration.eventConsumerRetryAdvice();
    var invocation = listenerInvocation();
    var rejection = new AmqpRejectAndDontRequeueException("malformed");
    when(invocation.proceed()).thenThrow(new IllegalStateException("listener failed", rejection));

    var thrown =
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> advice.invoke(invocation));

    assertSame(rejection, thrown);
    verify(invocation).proceed();
  }

  private EventConsumerRabbitConf configurationWithFastBackoff() {
    var configuration = new EventConsumerRabbitConf();
    ReflectionTestUtils.setField(configuration, "maxAttempts", 3);
    ReflectionTestUtils.setField(configuration, "initialInterval", 1L);
    ReflectionTestUtils.setField(configuration, "multiplier", 2.0);
    ReflectionTestUtils.setField(configuration, "maxInterval", 2L);
    return configuration;
  }

  private ProxyMethodInvocation listenerInvocation() {
    var invocation = mock(ProxyMethodInvocation.class);
    var message = new Message(new byte[0], new MessageProperties());
    when(invocation.getArguments()).thenReturn(new Object[] {mock(Channel.class), message});
    when(invocation.invocableClone()).thenReturn(invocation);
    return invocation;
  }
}
