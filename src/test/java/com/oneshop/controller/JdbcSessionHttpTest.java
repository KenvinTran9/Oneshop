package com.oneshop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import com.oneshop.MySqlIntegrationTestSupport;
import com.oneshop.dto.CartSelection;
import com.oneshop.service.CartService;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.servlet.session.cookie.secure=false")
@ActiveProfiles({"test", "jdbc-session-test"})
class JdbcSessionHttpTest extends MySqlIntegrationTestSupport {
    @LocalServerPort int port;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean JdbcIndexedSessionRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired com.oneshop.repository.UserRepository users;
    @Autowired com.oneshop.repository.RoleRepository roles;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;

    @Test void publicAssetsDoNotLoadJdbcSessionsEvenWhenTheBrowserHasASessionCookie() throws Exception {
        var client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        String base = "http://localhost:" + port;
        client.send(HttpRequest.newBuilder(URI.create(base + "/login")).GET().build(), HttpResponse.BodyHandlers.ofString());
        org.mockito.Mockito.clearInvocations(repository);
        for (String path : java.util.List.of("/css/home.css", "/js/shop.js", "/images/oneshop-smile.svg")) {
            var response = client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        }
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).findById(org.mockito.ArgumentMatchers.anyString());
        client.send(HttpRequest.newBuilder(URI.create(base + "/account/header")).GET().build(), HttpResponse.BodyHandlers.ofString());
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.atLeastOnce()).findById(org.mockito.ArgumentMatchers.anyString());
    }

    @Test void authenticatedSessionSurvivesRepositoryReplacementAndLogoutRemovesIt() throws Exception {
        var role = new com.oneshop.entity.Role(); role.setName("CUSTOMER"); role = roles.saveAndFlush(role);
        var user = new com.oneshop.entity.User(); user.setEmail("jdbc-session@oneshop.test");
        user.setFullName("Session test"); user.setPassword(encoder.encode("JdbcSession!2026"));
        user.setRole(role); user.setActive(true); users.saveAndFlush(user);
        var cookies = new CookieManager();
        var client = HttpClient.newBuilder().cookieHandler(cookies).build();
        String base = "http://localhost:" + port;
        var page = client.send(HttpRequest.newBuilder(URI.create(base + "/login")).GET().build(), HttpResponse.BodyHandlers.ofString());
        String anonymousId = sessionCookie(cookies);
        String body = "username=jdbc-session%40oneshop.test&password=JdbcSession%212026&_csrf=" + csrf(page.body());
        var login = client.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(302);
        String signedInId = sessionCookie(cookies);
        assertThat(signedInId).isNotEqualTo(anonymousId);
        var otherRepository = new JdbcIndexedSessionRepository(jdbc, new TransactionTemplate(transactions));
        org.springframework.session.Session stored = otherRepository.findById(signedInId);
        assertThat(stored).isNotNull();
        org.springframework.security.core.context.SecurityContext context = stored.getAttribute("SPRING_SECURITY_CONTEXT");
        assertThat(context.getAuthentication().getName()).isEqualTo("jdbc-session@oneshop.test");
        var confirmation = client.send(HttpRequest.newBuilder(URI.create(base + "/logout")).GET().build(), HttpResponse.BodyHandlers.ofString());
        var logout = client.send(HttpRequest.newBuilder(URI.create(base + "/logout"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("_csrf=" + csrf(confirmation.body()))).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(logout.statusCode()).isEqualTo(302);
        assertThat(otherRepository.findById(signedInId)).isNull();
    }

    private String sessionCookie(CookieManager cookies) {
        return cookies.getCookieStore().getCookies().stream().filter(cookie -> cookie.getName().equals("JSESSIONID"))
                .map(cookie -> new String(java.util.Base64.getDecoder().decode(cookie.getValue()),
                        java.nio.charset.StandardCharsets.UTF_8)).findFirst().orElseThrow();
    }

    private String csrf(String html) {
        var match = java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(match.find()).isTrue();
        return java.net.URLEncoder.encode(match.group(1), java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test void cookiesAndCsrfAreStoredInMySqlAndGuestCartCanBeLoadedByAnotherRepository() throws Exception {
        var cookies = new CookieManager();
        var client = HttpClient.newBuilder().cookieHandler(cookies).build();
        var page = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.headers().allValues("Set-Cookie").toString()).contains("JSESSIONID=", "HttpOnly", "SameSite=Lax");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class)).isGreaterThan(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES", Integer.class)).isGreaterThan(0);
        org.springframework.session.Session session = repository.createSession();
        var cart = new LinkedHashMap<CartSelection, Integer>();
        cart.put(new CartSelection(123L, "30 ml", "Hồng"), 2);
        session.setAttribute(CartService.GUEST_CART_SESSION_ATTRIBUTE, cart);
        @SuppressWarnings({"unchecked", "rawtypes"})
        org.springframework.session.SessionRepository<org.springframework.session.Session> writer =
                (org.springframework.session.SessionRepository) repository;
        writer.save(session);
        var otherRepository = new JdbcIndexedSessionRepository(jdbc, new TransactionTemplate(transactions));
        org.springframework.session.Session restored = otherRepository.findById(session.getId());
        assertThat(restored).isNotNull();
        Map<CartSelection, Integer> restoredCart = restored.getAttribute(CartService.GUEST_CART_SESSION_ATTRIBUTE);
        assertThat(restoredCart).containsEntry(new CartSelection(123L, "30 ml", "Hồng"), 2);
        repository.deleteById(session.getId());
        assertThat(otherRepository.findById(session.getId())).isNull();
    }
}
