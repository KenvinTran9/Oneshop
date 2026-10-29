package com.oneshop.service;

import com.oneshop.repository.OrderRepository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class OrderMailService {
    private final OrderRepository orders;
    private final OrderService orderService;
    private final JavaMailSender sender;
    private final SpringTemplateEngine templates;
    private final boolean enabled;
    private final String from, username, password, baseUrl;
    public OrderMailService(OrderRepository orders, OrderService orderService, JavaMailSender sender,
            SpringTemplateEngine templates, @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:}") String from, @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password, @Value("${app.mail.base-url:http://localhost:8080}") String baseUrl) {
        this.orders = orders; this.orderService = orderService; this.sender = sender; this.templates = templates;
        this.enabled = enabled; this.from = from; this.username = username; this.password = password;
        var uri = URI.create(baseUrl);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalStateException("APP_BASE_URL must be an absolute HTTP(S) URL");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendConfirmation(String code) {
        var order = orders.findForUpdate(code).orElseThrow();
        if ("SENT".equals(order.getEmailStatus()) || order.getStatus() == com.oneshop.entity.OrderStatus.CANCELLED) return;
        if (!enabled || from.isBlank() || username.isBlank() || password.isBlank()) {
            order.setEmailStatus("NOT_CONFIGURED"); return;
        }
        try {
            var view = orderService.view(order);
            String trackingUrl = baseUrl + "/orders/" + code;
            Context context = new Context(java.util.Locale.forLanguageTag("vi"));
            context.setVariable("order", view); context.setVariable("trackingUrl", trackingUrl);
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "OneShop"); helper.setTo(order.getCustomerEmail());
            helper.setSubject("OneShop — Đặt hàng thành công " + code);
            String text = "Chào " + view.customerName() + ",\nOneShop đã nhận đơn hàng " + code
                    + ".\nTổng tiền COD: " + view.totalAmount().toPlainString() + " VND.\nTheo dõi đơn: " + trackingUrl
                    + "\nVui lòng đăng nhập bằng tài khoản đã đặt hàng để xem chi tiết.";
            helper.setText(text, templates.process("mail/order-confirmation", context));
            sender.send(message);
            order.setEmailStatus("SENT"); order.setEmailSentAt(LocalDateTime.now());
        } catch (Exception failure) {
            order.setEmailStatus("FAILED");
            // Do not log SMTP exception messages: they can include account details.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Could not send confirmation for {} ({})", code, failure.getClass().getSimpleName());
        }
    }
}
