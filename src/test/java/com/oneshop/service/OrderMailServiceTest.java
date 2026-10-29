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
}
