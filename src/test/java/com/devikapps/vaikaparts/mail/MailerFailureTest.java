package com.devikapps.vaikaparts.mail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.config.EmailConf;
import com.devikapps.vaikaparts.exception.EmailSendException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class MailerFailureTest {
  @Test
  void strict_send_propagates_failure_while_legacy_accept_keeps_its_behavior() throws Exception {
    var sender = mock(JavaMailSender.class);
    var config = mock(EmailConf.class);
    when(config.getFromEmail()).thenReturn("notifications@example.com");
    when(sender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    doThrow(new MailSendException("SMTP unavailable")).when(sender).send(any(MimeMessage.class));
    var mailer = new Mailer(sender, config);
    var email = new Email(new InternetAddress("alice@example.com"), List.of(), List.of(),
        "Test", "<p>Test</p>", List.of());
    assertThrows(EmailSendException.class, () -> mailer.sendOrThrow(email));
    assertDoesNotThrow(() -> mailer.accept(email));
    assertThrows(EmailSendException.class, () -> mailer.sendOrThrow(null));
  }
}
