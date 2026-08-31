package com.personal.base.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  @Autowired
  private JavaMailSender mailSender;

  @Value("${app.mail.from}")
  private String fromAddress;

  @Async
  public void sendRegistrationSuccessEmail(String toEmail, String username) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(toEmail);
    message.setSubject("Đăng ký tài khoản thành công");
    message.setText("Xin chào " + username + ",\n\n"
            + "Bạn đã đăng ký tài khoản thành công trên hệ thống Learn English.\n"
            + "Chúc bạn học tập hiệu quả!\n\n"
            + "Trân trọng.");

    mailSender.send(message);
  }

  @Async
  public void sendNotificationEmail(String toEmail, String title, String content, String imageUrl, String link) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(toEmail);
      helper.setSubject(title);
      helper.setText(buildNotificationHtml(title, content, imageUrl, link), true);

      mailSender.send(mimeMessage);
    } catch (MessagingException e) {
      throw new IllegalStateException("Failed to send notification email to " + toEmail, e);
    }
  }

  private String buildNotificationHtml(String title, String content, String imageUrl, String link) {
    StringBuilder html = new StringBuilder();
    html.append("<div style=\"font-family:Arial,sans-serif;max-width:600px;\">");
    html.append("<h2>").append(title).append("</h2>");
    if (imageUrl != null && !imageUrl.isBlank()) {
      html.append("<img src=\"").append(imageUrl).append("\" style=\"max-width:100%;\" /><br/><br/>");
    }
    if (content != null) {
      html.append("<p>").append(content.replace("\n", "<br/>")).append("</p>");
    }
    if (link != null && !link.isBlank()) {
      html.append("<p><a href=\"").append(link).append("\">Xem chi tiết</a></p>");
    }
    html.append("</div>");
    return html.toString();
  }
}
