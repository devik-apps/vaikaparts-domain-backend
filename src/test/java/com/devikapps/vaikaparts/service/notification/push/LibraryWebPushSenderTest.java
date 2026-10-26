package com.devikapps.vaikaparts.service.notification.push;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.devikapps.vaikaparts.repository.model.notification.JWebPushSubscription;
import com.devikapps.vaikaparts.service.notification.WebPushProperties;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.*;
import java.util.Base64;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveGenParameterSpec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("unchecked")
class LibraryWebPushSenderTest {
  static WebPushProperties validProperties() throws Exception {
    Security.addProvider(new BouncyCastleProvider());
    var gen = KeyPairGenerator.getInstance("ECDH", "BC");
    gen.initialize(new ECNamedCurveGenParameterSpec("prime256v1"));
    var keys = gen.generateKeyPair();
    var p = new WebPushProperties();
    p.setEnabled(true);
    p.setPublicKey(
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(Utils.encode((ECPublicKey) keys.getPublic())));
    p.setPrivateKey(
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(Utils.encode((ECPrivateKey) keys.getPrivate())));
    p.setSubject("mailto:push@example.com");
    return p;
  }

  @Test
  void preparesEncryptedVapidRequestWithExistingBouncyCastleAndNoRedirects() throws Exception {
    var p = validProperties();
    var client = mock(HttpClient.class);
    var validator = mock(WebPushEndpointValidator.class);
    when(client.followRedirects()).thenReturn(HttpClient.Redirect.NEVER);
    HttpResponse<Void> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(307);
    when(client.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    var sender = new LibraryWebPushSender(p, client, validator);
    var sub =
        JWebPushSubscription.builder()
            .endpoint("https://push.example.com/endpoint")
            .p256dh(p.getPublicKey())
            .auth(Base64.getUrlEncoder().encodeToString(new byte[16]))
            .build();
    assertEquals(307, sender.send(sub, "hello".getBytes()));
    var request = ArgumentCaptor.forClass(HttpRequest.class);
    var order = inOrder(validator, client);
    order.verify(validator).validate(sub.getEndpoint());
    order.verify(client).send(request.capture(), any());
    assertEquals(
        "aes128gcm", request.getValue().headers().firstValue("Content-Encoding").orElseThrow());
    assertTrue(
        request
            .getValue()
            .headers()
            .firstValue("Authorization")
            .orElseThrow()
            .startsWith("vapid "));
    assertTrue(request.getValue().bodyPublisher().orElseThrow().contentLength() > 5);
  }

  @Test
  void rejectsRedirectFollowingClient() throws Exception {
    var client = mock(HttpClient.class);
    when(client.followRedirects()).thenReturn(HttpClient.Redirect.ALWAYS);
    assertThrows(
        IllegalArgumentException.class,
        () -> new LibraryWebPushSender(validProperties(), client, new WebPushEndpointValidator()));
  }

  @Test
  void mismatchedKeysAndInvalidSubjectAreRejectedWithoutSecrets() throws Exception {
    var p = validProperties();
    p.setPrivateKey(validProperties().getPrivateKey());
    var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    assertEquals(
        "Enabled web push requires valid VAPID public-key, private-key and subject",
        assertThrows(
                IllegalArgumentException.class,
                () -> new LibraryWebPushSender(p, client, new WebPushEndpointValidator()))
            .getMessage());
    p.setSubject("file:///secret");
    assertThrows(
        IllegalArgumentException.class,
        () -> new LibraryWebPushSender(p, client, new WebPushEndpointValidator()));
  }
}
