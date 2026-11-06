package com.plantmarket.catalog.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Best-effort restock notification: SMTP failures are logged and never break stock changes. */
@Component
public class RestockNotifier {

  private static final Logger log = LoggerFactory.getLogger(RestockNotifier.class);

  private final JavaMailSender mailSender;

  public RestockNotifier(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public boolean notifyAvailable(String email, String label) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(email);
      message.setSubject("Plant Market: снова в наличии — " + label);
      message.setText(
          "Товар «" + label + "» снова в наличии. Успейте оформить заказ на plant-market.by.");
      mailSender.send(message);
      log.info("Restock notification sent for {}", label);
      return true;
    } catch (MailException exception) {
      log.warn("Could not send restock notification for {}: {}", label, exception.getMessage());
      return false;
    }
  }
}
