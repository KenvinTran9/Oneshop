package com.oneshop.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.oneshop.MySqlIntegrationTestSupport;
import com.oneshop.entity.Role;
import com.oneshop.entity.User;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.UserRepository;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AccountPageHttpTest extends MySqlIntegrationTestSupport {
    @LocalServerPort private int port;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder encoder;

    @Test
    void realBrowserSessionRendersLogoutFormAndOnlyValidPostEndsIt() throws Exception {
        Role role = new Role(); role.setName("CUSTOMER"); role = roles.saveAndFlush(role);
        User user = new User(); user.setEmail("http-security@oneshop.test"); user.setFullName("HTTP security test");
        user.setPassword(encoder.encode("HttpSecurity!2026")); user.setRole(role); user.setActive(true); users.saveAndFlush(user);
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        String base = "http://localhost:" + port;
        var loginPage = client.send(HttpRequest.newBuilder(URI.create(base + "/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String cookies = String.join(";", loginPage.headers().allValues("Set-Cookie"));
        assertThat(cookies).contains("HttpOnly", "SameSite=Lax");
        String credentials = "username=http-security%40oneshop.test&password=HttpSecurity%212026&_csrf="
                + URLEncoder.encode(csrfToken(loginPage.body()), StandardCharsets.UTF_8);
        var loggedIn = client.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(credentials)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(loggedIn.statusCode()).isEqualTo(302);
        var confirmation = client.send(HttpRequest.newBuilder(URI.create(base + "/logout")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(confirmation.statusCode()).isEqualTo(200);
        assertThat(confirmation.body()).contains("Bạn muốn đăng xuất?", "site-header", "method=\"post\"");
        assertThat(confirmation.headers().firstValue("Content-Security-Policy").orElse("")).contains("form-action 'self'");
        var denied = client.send(HttpRequest.newBuilder(URI.create(base + "/logout"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(denied.statusCode()).isEqualTo(403);
        var stillSignedIn = client.send(HttpRequest.newBuilder(URI.create(base + "/account/status")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(stillSignedIn.statusCode()).isEqualTo(200);
        String body = "_csrf=" + URLEncoder.encode(csrfToken(confirmation.body()), StandardCharsets.UTF_8);
        var signedOut = client.send(HttpRequest.newBuilder(URI.create(base + "/logout"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(signedOut.statusCode()).isEqualTo(302);
        assertThat(signedOut.headers().firstValue("Location").orElse("")).endsWith("/login?logout");
        var anonymous = client.send(HttpRequest.newBuilder(URI.create(base + "/account/status")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(anonymous.statusCode()).isEqualTo(401);
    }

    private String csrfToken(String html) {
        var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

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
