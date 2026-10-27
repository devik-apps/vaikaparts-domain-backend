package com.devikapps.vaikaparts.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class ApiExceptionHandlerTest {
  final ApiExceptionHandler handler = new ApiExceptionHandler();
  final String path = "/v1/notifications/push-subscriptions";
  final ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", path));

  @Test
  void dataIntegrityLogsDoNotExposeEndpoint() {
    var endpoint = "https://push.example.com/task6-endpoint-sentinel-7b9c";
    var rootCause =
        new SQLException(
            "duplicate key value violates unique constraint: Key (endpoint)=(" + endpoint + ")");
    var exception = new DataIntegrityViolationException("Insert failed: " + endpoint, rootCause);
    var logger = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      var response = handler.handleDataIntegrityViolation(exception, request);

      assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
      assertEquals("DATA_CONFLICT", response.getBody().errorCode());
      assertEquals("A conflict occurred with existing data", response.getBody().message());
      assertEquals(path, response.getBody().path());
      assertEquals(1, appender.list.size());
      var event = appender.list.getFirst();
      assertFalse(event.getFormattedMessage().contains(endpoint), event.getFormattedMessage());
      assertNull(event.getThrowableProxy(), "Database exceptions must not be attached to logs");
      assertTrue(event.getFormattedMessage().contains(SQLException.class.getName()));
      assertTrue(event.getFormattedMessage().contains(path));
      assertTrue(event.getFormattedMessage().contains("DATA_CONFLICT"));
      assertFalse(event.getFormattedMessage().contains("duplicate key value"));
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @ParameterizedTest
  @CsvSource({
    "duplicate email unique constraint, DUPLICATE_EMAIL, Email already exists",
    "duplicate supabase_id unique constraint, DUPLICATE_SUPABASE_ID, Supabase ID already exists"
  })
  void duplicateClassificationRemainsUnchanged(String detail, String code, String message) {
    var response =
        handler.handleDataIntegrityViolation(
            new DataIntegrityViolationException("Insert failed", new SQLException(detail)),
            request);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals(code, response.getBody().errorCode());
    assertEquals(message, response.getBody().message());
  }

  @Test
  void missingRootCauseKeepsGenericConflictResponse() {
    var response =
        handler.handleDataIntegrityViolation(
            new DataIntegrityViolationException("Insert failed"), request);

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("DATA_CONFLICT", response.getBody().errorCode());
    assertEquals("A conflict occurred with existing data", response.getBody().message());
  }
}
