package com.plantmarket.order.notification;

import com.plantmarket.order.domain.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class OrderNotificationListener {

  private static final Logger log = LoggerFactory.getLogger(OrderNotificationListener.class);

  private final JavaMailSender mailSender;

  public OrderNotificationListener(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @EventListener
  public void onOrderStatusChanged(OrderStatusChangedEvent event) {
    log.info("Order status event: order={} status={}", event.orderId(), event.status());
    if (event.customerEmail() == null || event.customerEmail().isBlank()) {
      return;
    }
    if (event.status() != OrderStatus.PAID && event.status() != OrderStatus.CANCELLED) {
      return;
    }
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(event.customerEmail());
    message.setSubject("Plant Market: заказ " + event.orderId() + " — " + event.status());
    message.setText("Статус вашего заказа: " + event.status());
    mailSender.send(message);
  }
}
