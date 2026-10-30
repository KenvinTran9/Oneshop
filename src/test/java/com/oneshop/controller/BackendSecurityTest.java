package com.oneshop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneshop.MySqlIntegrationTestSupport;
import com.oneshop.entity.Role;
import com.oneshop.entity.User;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.UserRepository;
import com.oneshop.security.JwtService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendSecurityTest extends MySqlIntegrationTestSupport {
    private static final String EMAIL = "security-customer@oneshop.test";
    private static final String PASSWORD = "SecurityTest!2026";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder encoder;

    @Test void apiLoginIsStatelessAndBearerTokenAuthenticatesWithoutCsrf() throws Exception {
        account(true);
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "  SECURITY-CUSTOMER@ONESHOP.TEST  ", "password", PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
        String token = json.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.isAdmin").value(false));
        mvc.perform(get("/api/admin/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test void sessionCookiesCannotAuthenticateJwtApiEvenWithMalformedBearerHeader() throws Exception {
        account(true);
        var session = login();
        mvc.perform(get("/account/status").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").session(session).header("Authorization", "Bearer forged-token"))
                .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        // An invalid header to the website cannot clear a valid web session either.
        mvc.perform(get("/account/status").session(session).header("Authorization", "Bearer forged-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test void bearerTokenDoesNotCreateWebLoginOrBypassWebCsrf() throws Exception {
        account(true);
        String token = jwt.generateAccessToken(EMAIL);
        mvc.perform(get("/account/status").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/products/new").header("Authorization", "Bearer " + token)).andExpect(status().is3xxRedirection());
        mvc.perform(post("/cart/items").header("Authorization", "Bearer " + token)
                        .param("productId", "1").param("quantity", "1"))
                .andExpect(status().isForbidden());
    }

    @Test void accessAndRefreshTokensCannotBeSubstitutedAndDisabledUsersAreRejected() throws Exception {
        account(true);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt.generateRefreshToken(EMAIL)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", jwt.generateAccessToken(EMAIL)))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", jwt.generateRefreshToken(EMAIL)))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        User user = users.findByEmail(EMAIL).orElseThrow(); user.setActive(false); users.saveAndFlush(user);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt.generateAccessToken(EMAIL)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", jwt.generateRefreshToken(EMAIL)))))
                .andExpect(status().isUnauthorized());
    }

    @Test void invalidApiCredentialsAndTokenErrorsAreGenericJsonInsteadOfHtmlOrStacktraces() throws Exception {
        account(true);
        String expected = null;
        for (String email : new String[]{EMAIL, "unknown@oneshop.test"}) {
            var response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "password", "wrong-password"))))
                    .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse().getContentAsString();
            if (expected == null) expected = response; else assertThat(response).isEqualTo(expected);
            assertThat(response).doesNotContain(email, "Exception", "password");
        }
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"malformed\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").exists());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{broken-json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", EMAIL, "password", "x".repeat(1000)))))
                .andExpect(status().isBadRequest());
    }

    @Test void logoutRequiresPostAndCsrfWhileGetOnlyShowsConfirmation() throws Exception {
        account(true);
        var session = login();
        mvc.perform(get("/logout").session(session)).andExpect(status().isOk()).andExpect(view().name("pages/logout"));
        assertThat(session.isInvalid()).isFalse();
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
        mvc.perform(get("/account/status").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(redirectedUrl("/login?logout"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test void authenticationChangesSessionIdAndProtectiveHeadersArePresent() throws Exception {
        account(true);
        var session = new MockHttpSession(); String originalId = session.getId();
        mvc.perform(post("/login").session(session).with(csrf()).param("username", EMAIL).param("password", PASSWORD))
                .andExpect(redirectedUrl("/"));
        assertThat(session.getId()).isNotEqualTo(originalId);
        mvc.perform(get("/login"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("object-src 'none'")));
    }

    private MockHttpSession login() throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("username", EMAIL).param("password", PASSWORD))
                .andExpect(redirectedUrl("/")).andReturn().getRequest().getSession(false);
    }

    private void account(boolean active) {
        Role role = new Role(); role.setName("CUSTOMER"); role = roles.saveAndFlush(role);
        User user = new User(); user.setEmail(EMAIL); user.setFullName("Security test"); user.setPhone("0912345678");
        user.setRole(role); user.setPassword(encoder.encode(PASSWORD)); user.setActive(active); users.saveAndFlush(user);
    }
}
