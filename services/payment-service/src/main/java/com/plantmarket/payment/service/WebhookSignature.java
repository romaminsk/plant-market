package com.plantmarket.payment.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HMAC-SHA256 signature over timestamp, event id and the exact raw webhook body. */
public final class WebhookSignature {

  private static final String ALGORITHM = "HmacSHA256";

  private WebhookSignature() {}

  public static String compute(String secret, String timestamp, String eventId, String body) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
      mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
      mac.update((byte) '\n');
      mac.update(eventId.getBytes(StandardCharsets.UTF_8));
      mac.update((byte) '\n');
      mac.update(body.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(mac.doFinal());
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("HmacSHA256 is not available", exception);
    }
  }

  public static boolean matches(
      String secret, String timestamp, String eventId, String body, String signature) {
    if (signature == null || signature.isBlank()) {
      return false;
    }
    byte[] provided;
    try {
      provided = HexFormat.of().parseHex(signature);
    } catch (IllegalArgumentException exception) {
      return false;
    }
    byte[] expected = HexFormat.of().parseHex(compute(secret, timestamp, eventId, body));
    return MessageDigest.isEqual(expected, provided);
  }
}
