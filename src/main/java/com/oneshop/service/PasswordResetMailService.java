package com.oneshop.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class PasswordResetMailService {
    private final JavaMailSender smtp;
    private final ResendMailSender resend;
    private final SpringTemplateEngine templates;
    private final boolean enabled;
    private final String provider, from, username, password, baseUrl;
    public PasswordResetMailService(JavaMailSender smtp, ResendMailSender resend, SpringTemplateEngine templates,
            @Value("${app.mail.enabled:false}") boolean enabled, @Value("${app.mail.provider:smtp}") String provider,
            @Value("${app.mail.from:}") String from, @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password, @Value("${app.mail.base-url:http://localhost:8080}") String baseUrl) {
        this.smtp = smtp; this.resend = resend; this.templates = templates; this.enabled = enabled;
        this.provider = provider; this.from = from; this.username = username; this.password = password;
        var uri = URI.create(baseUrl);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalStateException("APP_BASE_URL must be an absolute HTTP(S) URL");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }
    public void send(PasswordResetService.Requested event) {
        try {
            boolean configured = "resend".equals(provider) ? resend.isConfigured() : !username.isBlank() && !password.isBlank();
            if (!enabled || from.isBlank() || !configured) throw new IllegalStateException("Mail unavailable");
            String url = baseUrl + "/reset-password?token=" + event.token();
            String subject = "OneShop — Xác thực đặt lại mật khẩu";
            String text = "Chào " + event.fullName() + ",\nĐặt lại mật khẩu OneShop tại: " + url
                    + "\nLiên kết có hiệu lực 15 phút và chỉ sử dụng một lần.\nNếu không yêu cầu, bạn có thể bỏ qua email này.";
            var context = new Context(java.util.Locale.forLanguageTag("vi"));
            context.setVariable("fullName", event.fullName()); context.setVariable("resetUrl", url);
            String html = templates.process("mail/password-reset", context);
            if ("resend".equals(provider)) resend.sendPasswordReset(from, event.email(), subject, text, html, event.deliveryId());
            else {
                var message = smtp.createMimeMessage();
                var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
                helper.setFrom(from, "OneShop"); helper.setTo(event.email()); helper.setSubject(subject); helper.setText(text, html);
                smtp.send(message);
            }
        } catch (Exception failure) {
            // No email address, reset URL, token or provider response body may enter logs.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Password reset mail could not be sent ({})", failure.getClass().getSimpleName());
        }
    }
}
