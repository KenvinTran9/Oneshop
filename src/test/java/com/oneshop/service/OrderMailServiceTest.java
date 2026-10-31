package com.oneshop.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.oneshop.entity.Order;
import com.oneshop.repository.OrderRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;

class OrderMailServiceTest {
    @Test void missingSmtpConfigurationIsRecordedWithoutAttemptingDelivery() {
        var repository=mock(OrderRepository.class); var sender=mock(JavaMailSender.class);
        Order order=new Order(); when(repository.findForUpdate("OS-TEST")).thenReturn(Optional.of(order));
        new OrderMailService(repository,mock(OrderService.class),sender,mock(SpringTemplateEngine.class),
                false,"","","","http://localhost:8080").sendConfirmation("OS-TEST");
        assertThat(order.getEmailStatus()).isEqualTo("NOT_CONFIGURED"); verifyNoInteractions(sender);
    }
    @Test void sentOrdersNeverSendAnotherConfirmation() {
        var repository=mock(OrderRepository.class); var sender=mock(JavaMailSender.class);
        Order order=new Order(); order.setEmailStatus("SENT");
        when(repository.findForUpdate("OS-TEST")).thenReturn(Optional.of(order));
        new OrderMailService(repository,mock(OrderService.class),sender,mock(SpringTemplateEngine.class),
                true,"shop@example.com","shop@example.com","test","http://localhost:8080").sendConfirmation("OS-TEST");
        verifyNoInteractions(sender);
    }
    @Test void emailLinksRequireAnExplicitHttpOrigin() {
        assertThatThrownBy(()->new OrderMailService(mock(OrderRepository.class),mock(OrderService.class),
                mock(JavaMailSender.class),mock(SpringTemplateEngine.class),false,"","","","javascript:alert(1)"))
                .isInstanceOf(IllegalStateException.class);
    }
    private OrderUpdateMail update() {
        return new OrderUpdateMail("event-123", "OS-TEST", "buyer@example.com", "Khách thử",
                "Đang giao hàng", "Đơn đã được bàn giao.", "Đang giao hàng", "Đã thanh toán",
                "Giao hàng thử", "Nguyễn An", "0901234567", "TRACK-01", "01/11/2026", "Gọi trước khi giao");
    }
    @Test void updateSendsShipperDetailsThroughSmtp() throws Exception {
        var sender = mock(JavaMailSender.class); var templates = mock(SpringTemplateEngine.class);
        var mime = new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new java.util.Properties()));
        when(sender.createMimeMessage()).thenReturn(mime);
        when(templates.process(eq("mail/order-update"), any(org.thymeleaf.context.IContext.class))).thenReturn("<p>Đang giao hàng</p>");
        var service = new OrderMailService(mock(OrderRepository.class), mock(OrderService.class), sender, templates,
                true, "shop@example.com", "shop@example.com", "test", "https://shop.example.com");
        service.sendUpdate(update());
        verify(sender).send(mime);
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("buyer@example.com");
        assertThat(mime.getSubject()).contains("Đang giao hàng", "OS-TEST");
        assertThat(mailText(mime.getContent())).contains("0901234567", "TRACK-01", "https://shop.example.com/orders/OS-TEST");
    }
    @Test void resendUpdatesUseSeparateIdempotencyKeysFromOrderConfirmation() throws Exception {
        var resend = mock(ResendMailSender.class); when(resend.isConfigured()).thenReturn(true);
        var sender = mock(JavaMailSender.class); var templates = mock(SpringTemplateEngine.class);
        when(templates.process(eq("mail/order-update"), any(org.thymeleaf.context.IContext.class))).thenReturn("<p>Update</p>");
        new OrderMailService(mock(OrderRepository.class), mock(OrderService.class), sender, templates,
                true, "onboarding@resend.dev", "", "", "https://shop.example.com", "resend", resend).sendUpdate(update());
        verify(resend).sendOrderUpdate(eq("onboarding@resend.dev"), eq("buyer@example.com"), contains("OS-TEST"),
                contains("0901234567"), eq("<p>Update</p>"), eq("event-123"));
        verifyNoInteractions(sender);
    }
    @Test void listenerContainsTransportFailureSoCommittedOrderStillSucceeds() throws Exception {
        var service = mock(OrderMailService.class); var event = update();
        doThrow(new org.springframework.mail.MailSendException("test failure")).when(service).sendUpdate(event);
        assertThatCode(() -> new OrderMailListener(service).updated(event)).doesNotThrowAnyException();
    }

    private String mailText(Object content) throws Exception {
        if (content instanceof jakarta.mail.Multipart multipart) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) result.append(mailText(multipart.getBodyPart(i).getContent()));
            return result.toString();
        }
        return content.toString();
    }

    @Test void restockMailUsesRecipientAccountAndItsOwnResendKey() throws Exception {
        var resend=mock(ResendMailSender.class);when(resend.isConfigured()).thenReturn(true);
        var templates=mock(SpringTemplateEngine.class);
        when(templates.process(eq("mail/product-restock"),any(org.thymeleaf.context.IContext.class))).thenReturn("<p>Restock</p>");
        var service=new OrderMailService(mock(OrderRepository.class),mock(OrderService.class),mock(JavaMailSender.class),templates,
                true,"onboarding@resend.dev","","","https://shop.example.com","resend",resend);
        assertThat(service.sendRestock("buyer@example.com","Khách","Son","son-moi","watch-001")).isTrue();
        verify(resend).sendProductRestock(eq("onboarding@resend.dev"),eq("buyer@example.com"),contains("đã có hàng"),
                contains("https://shop.example.com/products/son-moi"),eq("<p>Restock</p>"),eq("watch-001"));
    }
    @Test void unconfiguredRestockMailStaysUnsent() throws Exception {
        var sender=mock(JavaMailSender.class);
        var service=new OrderMailService(mock(OrderRepository.class),mock(OrderService.class),sender,mock(SpringTemplateEngine.class),
                false,"","","","http://localhost:8080");
        assertThat(service.sendRestock("buyer@example.com","Khách","Son","son","watch-001")).isFalse();
        verifyNoInteractions(sender);
    }

}
