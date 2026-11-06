package com.plantmarket.order.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Best-effort care reminder: SMTP failures are logged and the reminder is retried next cycle. */
@Component
public class CareReminderNotifier {

  private static final Logger log = LoggerFactory.getLogger(CareReminderNotifier.class);

  private final JavaMailSender mailSender;

  public CareReminderNotifier(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public boolean send(String email, String plantName) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(email);
      message.setSubject("Plant Market: пора позаботиться о " + plantName);
      message.setText(
          "Напоминаем: проверьте полив, свет и состояние растения «"
              + plantName
              + "». Отключить напоминания можно в личном кабинете.");
      mailSender.send(message);
      log.info("Care reminder sent for {}", plantName);
      return true;
    } catch (MailException exception) {
      log.warn("Could not send care reminder for {}: {}", plantName, exception.getMessage());
      return false;
    }
  }
}
