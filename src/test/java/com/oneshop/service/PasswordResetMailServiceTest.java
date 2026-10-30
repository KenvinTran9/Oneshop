package com.oneshop.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class PasswordResetMailServiceTest {
    private SpringTemplateEngine templates() {
        var resolver = new ClassLoaderTemplateResolver(); resolver.setPrefix("templates/"); resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8"); resolver.setTemplateMode("HTML");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver); return engine;
    }
    private PasswordResetService.Requested event() {
        return new PasswordResetService.Requested("registered@oneshop.test", "Khách <b>thử</b>", "A".repeat(43), "test-delivery-id");
    }
    @Test void resendSendsOnlyToRegisteredAccountWithTrustedBaseUrlAndEscapedHtml() throws Exception {
        var smtp = mock(JavaMailSender.class); var resend = mock(ResendMailSender.class);
        when(resend.isConfigured()).thenReturn(true);
        var service = new PasswordResetMailService(smtp,resend,templates(),true,"resend","onboarding@resend.dev","","","https://oneshop.test/");
        service.send(event());
        var html = ArgumentCaptor.forClass(String.class);
        verify(resend).sendPasswordReset(eq("onboarding@resend.dev"),eq("registered@oneshop.test"),
                contains("đặt lại mật khẩu"),anyString(),html.capture(),eq("test-delivery-id"));
        assertThat(html.getValue()).contains("https://oneshop.test/reset-password?token=" + "A".repeat(43), "15 phút", "&lt;b&gt;thử&lt;/b&gt;")
                .doesNotContain("<b>thử</b>");
        verifyNoInteractions(smtp);
        assertThat(event().toString()).doesNotContain(event().token(), event().email());
    }
    @Test void smtpSetsTheRegisteredRecipientAndUsesTheSameVerificationLink() throws Exception {
        var smtp = mock(JavaMailSender.class); var resend = mock(ResendMailSender.class);
        var message = new MimeMessage(Session.getInstance(new Properties())); when(smtp.createMimeMessage()).thenReturn(message);
        new PasswordResetMailService(smtp,resend,templates(),true,"smtp","shop@oneshop.test","sender","test-only","https://oneshop.test")
                .send(event());
        verify(smtp).send(message);
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("registered@oneshop.test");
        assertThat(message.getSubject()).contains("đặt lại mật khẩu"); verifyNoInteractions(resend);
    }
    @Test void disabledTransportDoesNotSendAndProviderFailureDoesNotEscape() throws Exception {
        var smtp = mock(JavaMailSender.class); var resend = mock(ResendMailSender.class);
        new PasswordResetMailService(smtp,resend,templates(),false,"smtp","shop@oneshop.test","sender","test-only","https://oneshop.test")
                .send(event());
        verifyNoInteractions(smtp,resend);
        when(resend.isConfigured()).thenReturn(true);
        doThrow(new java.io.IOException("test-only provider failure")).when(resend).sendPasswordReset(anyString(),anyString(),anyString(),anyString(),anyString(),anyString());
        assertThatCode(() -> new PasswordResetMailService(smtp,resend,templates(),true,"resend","onboarding@resend.dev","","","https://oneshop.test")
                .send(event())).doesNotThrowAnyException();
    }
}
