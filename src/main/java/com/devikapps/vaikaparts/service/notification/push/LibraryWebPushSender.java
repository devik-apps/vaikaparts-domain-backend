package com.devikapps.vaikaparts.service.notification.push;

import com.devikapps.vaikaparts.repository.model.notification.JWebPushSubscription;
import com.devikapps.vaikaparts.service.notification.WebPushProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Security;
import java.time.Duration;
import nl.martijndwars.webpush.AbstractPushService;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/** Library owns RFC 8291/8292; this adapter owns outbound HTTP policy. */
public final class LibraryWebPushSender extends AbstractPushService<LibraryWebPushSender>
    implements WebPushSender {
  private final HttpClient client;
  private final WebPushEndpointValidator validator;

  public LibraryWebPushSender(
      WebPushProperties properties, HttpClient client, WebPushEndpointValidator validator) {
    if (client.followRedirects() != HttpClient.Redirect.NEVER)
      throw new IllegalArgumentException("Web push redirects must be disabled");
    this.client = client;
    this.validator = validator;
    try {
      Security.addProvider(new BouncyCastleProvider());
      if (properties.getPublicKey() == null
          || properties.getPublicKey().isBlank()
          || properties.getPrivateKey() == null
          || properties.getPrivateKey().isBlank()) throw new IllegalArgumentException();
      var subject = URI.create(properties.getSubject());
      if (!("mailto".equalsIgnoreCase(subject.getScheme())
              && subject.getSchemeSpecificPart().contains("@"))
          && !(("https".equalsIgnoreCase(subject.getScheme())
                  || "http".equalsIgnoreCase(subject.getScheme()))
              && subject.getHost() != null
              && subject.getUserInfo() == null)) throw new IllegalArgumentException();
      setPublicKey(properties.getPublicKey());
      setPrivateKey(properties.getPrivateKey());
      setSubject(properties.getSubject());
      if (!Utils.verifyKeyPair(getPrivateKey(), getPublicKey()))
        throw new IllegalArgumentException();
    } catch (Exception exception) {
      throw new IllegalArgumentException(
          "Enabled web push requires valid VAPID public-key, private-key and subject");
    }
  }

  @Override
  public int send(JWebPushSubscription subscription, byte[] payload) throws Exception {
    var push =
        new nl.martijndwars.webpush.Notification(
            subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(), payload);
    var prepared = prepareRequest(push, Encoding.AES128GCM);
    var request =
        HttpRequest.newBuilder(URI.create(prepared.getUrl()))
            .timeout(Duration.ofSeconds(20))
            .POST(HttpRequest.BodyPublishers.ofByteArray(prepared.getBody()));
    prepared.getHeaders().forEach(request::header);
    var outbound = request.build();
    // Validate the library's final URL immediately before the actual network call, too.
    validator.validate(outbound.uri().toString());
    return client.send(outbound, HttpResponse.BodyHandlers.discarding()).statusCode();
  }
}
