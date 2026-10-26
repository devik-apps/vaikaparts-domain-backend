package com.devikapps.vaikaparts.service.notification.push;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Shared by registration and delivery; validate again immediately before each outbound request. */
@Component
public class WebPushEndpointValidator {
  public void validate(String endpoint) {
    try {
      if (endpoint == null || endpoint.length() > 2048) throw unsafe();
      var uri = new URI(endpoint);
      var host = uri.getHost();
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || host == null
          || uri.getRawUserInfo() != null
          || uri.getRawFragment() != null
          || uri.getPort() > 65535) throw unsafe();
      var normalized = host.toLowerCase(Locale.ROOT).replaceAll("\\.$", "");
      if (normalized.equals("localhost")
          || normalized.endsWith(".localhost")
          || normalized.endsWith(".local")) throw unsafe();
      var addresses = InetAddress.getAllByName(host);
      if (addresses.length == 0) throw unsafe();
      for (var address : addresses) {
        if (!isPublic(address)) throw unsafe();
      }
    } catch (URISyntaxException | UnknownHostException ex) {
      throw unsafe();
    }
  }

  private boolean isPublic(InetAddress address) {
    if (address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress()) return false;
    byte[] bytes = address.getAddress();
    int first = bytes[0] & 255;
    int second = bytes[1] & 255;
    if (bytes.length == 16) return (first & 0xe0) == 0x20;
    return first != 0
        && first != 10
        && first != 127
        && first < 224
        && !(first == 100 && second >= 64 && second <= 127)
        && !(first == 169 && second == 254)
        && !(first == 172 && second >= 16 && second <= 31)
        && !(first == 192 && second == 168)
        && !(first == 198 && (second == 18 || second == 19));
  }

  private IllegalArgumentException unsafe() {
    return new IllegalArgumentException(
        "Push endpoint must use HTTPS and a public internet address");
  }
}
