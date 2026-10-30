package com.oneshop.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ResendMailSenderTest {
    @Test void sendsViaHttpsWithAuthenticationAndStableIdempotencyKey() throws Exception {
        var client = mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200); when(response.body()).thenReturn("{\"id\":\"mail-test\"}");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var sender = new ResendMailSender("test-only-key", new ObjectMapper(), client);
        sender.send("onboarding@resend.dev", "owner@example.test", "Đơn hàng", "Nội dung", "<p>Nội dung</p>", "OS-TEST");
        var request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(request.getValue().uri().toString()).isEqualTo("https://api.resend.com/emails");
        assertThat(request.getValue().headers().firstValue("Authorization")).contains("Bearer test-only-key");
        assertThat(request.getValue().headers().firstValue("Idempotency-Key")).contains("oneshop-confirmation-OS-TEST");
        assertThat(request.getValue().timeout()).isPresent();
    }

    @Test void providerFailureDoesNotExposeItsResponseBody() throws Exception {
        var client = mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(403); when(response.body()).thenReturn("sensitive-provider-response");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var sender = new ResendMailSender("test-only-key", new ObjectMapper(), client);
        assertThatThrownBy(() -> sender.send("shop@example.test", "owner@example.test", "Test", "Text", "<p>Text</p>", "OS-TEST"))
                .isInstanceOf(java.io.IOException.class).hasMessageContaining("403").hasMessageNotContaining("sensitive");
    }

    @Test void emptyKeyIsNotConfigured() {
        assertThat(new ResendMailSender("", new ObjectMapper(), mock(HttpClient.class)).isConfigured()).isFalse();
    }
}
