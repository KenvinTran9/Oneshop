package com.oneshop.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.oneshop.MySqlIntegrationTestSupport;
import com.oneshop.entity.*;
import com.oneshop.repository.*;
import com.oneshop.security.JwtService;
import com.oneshop.service.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetFlowTest extends MySqlIntegrationTestSupport {
    static final String EMAIL = "reset-buyer@oneshop.test", OLD = "OldPassword!2026", NEW = "NewPassword!2026";
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordResetAccountRepository accounts;
    @Autowired PasswordResetService resets;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    @Autowired MockMvc mvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @MockitoBean PasswordResetMailService mail;
    @LocalServerPort int port;
    User buyer;

    @BeforeEach void fixture() {
        org.mockito.Mockito.reset(mail);
        var role = new Role(); role.setName("CUSTOMER"); role = roles.saveAndFlush(role);
        buyer = new User(); buyer.setEmail(EMAIL); buyer.setFullName("Khách <b>thử</b>");
        buyer.setPassword(encoder.encode(OLD)); buyer.setRole(role); buyer.setActive(true); buyer = users.saveAndFlush(buyer);
    }
    PasswordResetService.Requested request() {
        resets.request("  RESET-BUYER@ONESHOP.TEST  ");
        var event = ArgumentCaptor.forClass(PasswordResetService.Requested.class);
        verify(mail, timeout(2000)).send(event.capture());
        return event.getValue();
    }
    PasswordResetAccount state() { return accounts.findById(buyer.getId()).orElseThrow(); }

    @Test void tokenIsHashedExpiresAfterFifteenMinutesAndCanOnlyChangeItsOwnersPasswordOnce() {
        String token = request().token();
        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(state().getTokenHash()).matches("[a-f0-9]{64}").isNotEqualTo(token);
        assertThat(state().getExpiresAt()).isBetween(Instant.now().plusSeconds(890), Instant.now().plusSeconds(901));
        assertThat(resets.isUsable(token)).isTrue(); assertThat(resets.isUsable(token)).isTrue();
        resets.reset(token, NEW);
        assertThat(encoder.matches(NEW, users.findByEmail(EMAIL).orElseThrow().getPassword())).isTrue();
        assertThat(encoder.matches(OLD, users.findByEmail(EMAIL).orElseThrow().getPassword())).isFalse();
        assertThat(state().getTokenHash()).isNull(); assertThat(state().getCredentialVersion()).isEqualTo(1);
        assertThat(resets.isUsable(token)).isFalse();
        assertThatThrownBy(() -> resets.reset(token, "DifferentPassword!2026")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void expiredOrReplacedTokenFailsAndPerAccountCooldownPreventsDuplicateMail() {
        String first = request().token();
        resets.request(EMAIL); verify(mail, times(1)).send(any());
        var state = state(); state.setExpiresAt(Instant.now().minusSeconds(1));
        state.setRequestedAt(Instant.now().minusSeconds(61)); accounts.saveAndFlush(state);
        assertThat(resets.isUsable(first)).isFalse();
        assertThatThrownBy(() -> resets.reset(first, NEW)).isInstanceOf(IllegalArgumentException.class);
        org.mockito.Mockito.reset(mail);
        String second = request().token();
        assertThat(second).isNotEqualTo(first); assertThat(resets.isUsable(first)).isFalse();
        assertThat(resets.isUsable(second)).isTrue();
        assertThat(encoder.matches(OLD, users.findByEmail(EMAIL).orElseThrow().getPassword())).isTrue();
    }
    @Test void unknownAndDisabledAccountsHaveSamePublicAcknowledgmentAndDoNotSendMail() throws Exception {
        buyer.setActive(false); users.saveAndFlush(buyer);
        for (String email : List.of(EMAIL, "unknown@oneshop.test")) {
            mvc.perform(post("/forgot-password").with(csrf()).param("email", email)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/forgot-password")).andExpect(flash().attribute("resetRequested", true));
        }
        assertThat(accounts.count()).isZero(); verifyNoInteractions(mail);
        mvc.perform(post("/forgot-password").param("email", EMAIL)).andExpect(status().isForbidden());
        mvc.perform(post("/forgot-password").with(csrf()).param("email", "not-an-email")).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("forgotForm", "email"));
    }
    @Test void concurrentSubmissionsConsumeTheTokenExactlyOnce() throws Exception {
        String token = request().token();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1); var results = new ArrayList<Future<Boolean>>();
            for (int i=0;i<2;i++) results.add(pool.submit(() -> {
                start.await(); try { resets.reset(token, NEW); return true; } catch (IllegalArgumentException expected) { return false; }
            }));
            start.countDown(); int success = 0;
            for (var result : results) if (result.get(15, TimeUnit.SECONDS)) success++;
            assertThat(success).isEqualTo(1);
        }
        assertThat(state().getCredentialVersion()).isEqualTo(1);
    }

    @Test void realResetFormChecksConfirmationAndCsrfAndRevokesOldWebAndJwtCredentials() throws Exception {
        HttpClient signedIn = client();
        assertThat(login(signedIn, OLD)).isEqualTo(302);
        assertThat(httpGet(signedIn, "/account/status").statusCode()).isEqualTo(200);
        String oldAccess = jwt.generateAccessToken(EMAIL), oldRefresh = jwt.generateRefreshToken(EMAIL);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + oldAccess)).andExpect(status().isOk());
        String token = request().token();
        HttpClient anonymous = client();
        var form = httpGet(anonymous, "/reset-password?token=" + token);
        assertThat(form.statusCode()).isEqualTo(200);
        assertThat(form.headers().firstValue("Referrer-Policy")).contains("no-referrer");
        assertThat(form.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        assertThat(form.body()).contains("Lưu mật khẩu mới", "name=\"token\"");
        var missingCsrf = httpPost(anonymous, "/reset-password", "token=" + token + "&password=" + encode(NEW) + "&confirmPassword=" + encode(NEW));
        assertThat(missingCsrf.statusCode()).isEqualTo(403);
        var mismatch = httpPost(anonymous, "/reset-password", "token=" + token + "&password=" + encode(NEW)
                + "&confirmPassword=DifferentPassword&_csrf=" + csrfValue(form.body()));
        assertThat(mismatch.statusCode()).isEqualTo(200);
        assertThat(mismatch.body()).contains("Mật khẩu xác nhận chưa khớp.").doesNotContain("value=\"" + NEW + "\"");
        assertThat(resets.isUsable(token)).isTrue();
        var success = httpPost(anonymous, "/reset-password", "token=" + token + "&password=" + encode(NEW)
                + "&confirmPassword=" + encode(NEW) + "&_csrf=" + csrfValue(mismatch.body()));
        assertThat(success.statusCode()).isEqualTo(302);
        assertThat(success.headers().firstValue("Location").orElse("")).endsWith("/login");
        assertThat(httpGet(signedIn, "/account/status").statusCode()).isEqualTo(401);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + oldAccess)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", oldRefresh)))).andExpect(status().isUnauthorized());
        var api = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", EMAIL, "password", NEW)))).andExpect(status().isOk()).andReturn();
        String newAccess = json.readTree(api.getResponse().getContentAsString()).path("accessToken").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + newAccess)).andExpect(status().isOk());
        var rejected = client(); assertThat(login(rejected, OLD)).isEqualTo(302);
        assertThat(httpGet(rejected, "/account/status").statusCode()).isEqualTo(401);
        var fresh = client(); assertThat(login(fresh, NEW)).isEqualTo(302);
        assertThat(httpGet(fresh, "/account/status").statusCode()).isEqualTo(200);
        assertThat(httpGet(anonymous, "/reset-password?token=" + token).body()).contains("Yêu cầu liên kết mới")
                .doesNotContain("id=\"password\"");
    }

    private HttpClient client() { return HttpClient.newBuilder().cookieHandler(new CookieManager()).build(); }
    private HttpResponse<String> httpGet(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> httpPost(HttpClient client, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private int login(HttpClient client, String password) throws Exception {
        return httpPost(client, "/login", "username=" + encode(EMAIL) + "&password=" + encode(password) + "&_csrf=" + csrfValue(httpGet(client, "/login").body())).statusCode();
    }
    private String csrfValue(String html) {
        var match = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(match.find()).isTrue(); return encode(match.group(1));
    }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
