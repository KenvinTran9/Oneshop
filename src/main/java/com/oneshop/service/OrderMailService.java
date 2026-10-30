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
    private final String from, username, password, baseUrl, provider;
    private final ResendMailSender resend;
    @org.springframework.beans.factory.annotation.Autowired
    public OrderMailService(OrderRepository orders, OrderService orderService, JavaMailSender sender,
            SpringTemplateEngine templates, @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:}") String from, @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password, @Value("${app.mail.base-url:http://localhost:8080}") String baseUrl,
            @Value("${app.mail.provider:smtp}") String provider, ResendMailSender resend) {
        this.orders = orders; this.orderService = orderService; this.sender = sender; this.templates = templates;
        this.enabled = enabled; this.from = from; this.username = username; this.password = password;
        this.provider = provider; this.resend = resend;
        if (!java.util.Set.of("smtp", "resend").contains(provider)) throw new IllegalStateException("Unknown MAIL_PROVIDER");
        var uri = URI.create(baseUrl);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalStateException("APP_BASE_URL must be an absolute HTTP(S) URL");
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    // Existing callers/tests retain their SMTP behavior.
    public OrderMailService(OrderRepository orders, OrderService orderService, JavaMailSender sender,
            SpringTemplateEngine templates, boolean enabled, String from, String username,
            String password, String baseUrl) {
        this(orders, orderService, sender, templates, enabled, from, username, password, baseUrl, "smtp", null);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendConfirmation(String code) {
        var order = orders.findForUpdate(code).orElseThrow();
        if ("SENT".equals(order.getEmailStatus()) || order.getStatus() == com.oneshop.entity.OrderStatus.CANCELLED) return;
        boolean transportConfigured = "resend".equals(provider) ? resend != null && resend.isConfigured()
                : !username.isBlank() && !password.isBlank();
        if (!enabled || from.isBlank() || !transportConfigured) {
            order.setEmailStatus("NOT_CONFIGURED"); return;
        }
        try {
            var view = orderService.view(order);
            String trackingUrl = baseUrl + "/orders/" + code;
            Context context = new Context(java.util.Locale.forLanguageTag("vi"));
            context.setVariable("order", view); context.setVariable("trackingUrl", trackingUrl);
            String subject = "OneShop — Đặt hàng thành công " + code;
            String text = "Chào " + view.customerName() + ",\nOneShop đã nhận đơn hàng " + code
                    + ".\nTổng tiền COD: " + view.totalAmount().toPlainString() + " VND.\nTheo dõi đơn: " + trackingUrl
                    + "\nVui lòng đăng nhập bằng tài khoản đã đặt hàng để xem chi tiết.";
            String html = templates.process("mail/order-confirmation", context);
            if ("resend".equals(provider)) {
                resend.send(from, order.getCustomerEmail(), subject, text, html, code);
            } else {
                var message = sender.createMimeMessage();
                var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
                helper.setFrom(from, "OneShop"); helper.setTo(order.getCustomerEmail());
                helper.setSubject(subject); helper.setText(text, html);
                sender.send(message);
            }
            order.setEmailStatus("SENT"); order.setEmailSentAt(LocalDateTime.now());
        } catch (Exception failure) {
            order.setEmailStatus("FAILED");
            // Do not log SMTP exception messages: they can include account details.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Could not send confirmation for {} ({})", code, failure.getClass().getSimpleName());
        }
    }
}
