package com.oneshop.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** HTTPS email transport; API keys and response bodies must never be logged. */
@Service
public class ResendMailSender {
    private final String apiKey;
    private final ObjectMapper json;
    private final HttpClient client;
    private static final URI ENDPOINT = URI.create("https://api.resend.com/emails");

    @org.springframework.beans.factory.annotation.Autowired
    public ResendMailSender(@Value("${app.mail.resend-api-key:}") String apiKey, ObjectMapper json) {
        this(apiKey, json, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    ResendMailSender(String apiKey, ObjectMapper json, HttpClient client) {
        this.apiKey = apiKey; this.json = json; this.client = client;
    }

    public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }

    public void send(String from, String to, String subject, String text, String html, String orderCode) throws Exception {
        sendMessage(from, to, subject, text, html, "oneshop-confirmation-" + orderCode);
    }
    public void sendPasswordReset(String from, String to, String subject, String text, String html, String deliveryId) throws Exception {
        sendMessage(from, to, subject, text, html, "oneshop-password-reset-" + deliveryId);
    }
    private void sendMessage(String from, String to, String subject, String text, String html, String idempotencyKey) throws Exception {
        var payload = Map.of("from", "OneShop <" + from + ">", "to", List.of(to),
                "subject", subject, "text", text, "html", html);
        var request = HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(12))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new java.io.IOException("Email provider rejected request (HTTP " + response.statusCode() + ")");
        if (!json.readTree(response.body()).path("id").isTextual())
            throw new java.io.IOException("Email provider did not confirm acceptance");
    }
}
