package com.plantmarket.order.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ClaimNotificationListener {

  private static final Logger log = LoggerFactory.getLogger(ClaimNotificationListener.class);

  private final JavaMailSender mailSender;

  public ClaimNotificationListener(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onClaimDecided(ClaimDecidedEvent event) {
    log.info(
        "Claim decided event: claim={} order={} status={}",
        event.claimId(),
        event.orderId(),
        event.status());
    if (event.customerEmail() == null || event.customerEmail().isBlank()) {
      return;
    }
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(event.customerEmail());
      message.setSubject("Plant Market: претензия " + event.claimId() + " — " + event.status());
      message.setText("Статус вашей претензии: " + event.status());
      mailSender.send(message);
    } catch (MailException exception) {
      log.warn(
          "Could not send claim notification for {}: {}", event.claimId(), exception.getMessage());
    }
  }
}
