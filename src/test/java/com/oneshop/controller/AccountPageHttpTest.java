package com.oneshop.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.oneshop.MongoIntegrationTestSupport;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AccountPageHttpTest extends MongoIntegrationTestSupport {
    @LocalServerPort private int port;

    @Test
    void invalidRegistrationKeepsDecoratedFormAndFieldErrorsOverRealHttp() throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        URI uri = URI.create("http://localhost:" + port + "/register");
        HttpResponse<String> page = client.send(HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(page.statusCode()).isEqualTo(200);
        var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(page.body());
        assertThat(matcher.find()).isTrue();
        String body = "fullName=Test&email=ui-test%40oneshop.test&phone=12345"
                + "&password=TestPassword42&confirmPassword=TestPassword42&_csrf="
                + URLEncoder.encode(matcher.group(1), StandardCharsets.UTF_8);
        HttpResponse<String> invalid = client.send(HttpRequest.newBuilder(uri)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(invalid.statusCode()).isEqualTo(200);
        assertThat(invalid.body()).contains("site-header", "account-shell",
                "Số điện thoại phải có định dạng Việt Nam hợp lệ", "phone-error");
        assertThat(invalid.body()).doesNotContain("value=\"TestPassword42\"");
        assertThat(invalid.headers().firstValue("Location")).isEmpty();
    }
}
