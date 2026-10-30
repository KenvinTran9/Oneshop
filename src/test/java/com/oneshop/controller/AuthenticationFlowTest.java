package com.oneshop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.oneshop.MySqlIntegrationTestSupport;
import com.oneshop.entity.Role;
import com.oneshop.entity.User;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationFlowTest extends MySqlIntegrationTestSupport {
    private static final String PASSWORD = "OneShopTest!2026";
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private PasswordEncoder encoder;

    @Test
    void registeredCustomerCanLoginWithMySqlCredentialsButCannotPublishProducts() throws Exception {
        role("CUSTOMER");
        mvc.perform(post("/register").with(csrf())
                        .param("fullName", "Khách OneShop").param("phone", "0912345678")
                        .param("email", "new-customer@oneshop.test")
                        .param("password", PASSWORD).param("confirmPassword", PASSWORD)
                        .param("role.name", "ADMIN").param("active", "false"))
                .andExpect(redirectedUrl("/login"));
        User stored = users.findByEmail("new-customer@oneshop.test").orElseThrow();
        assertThat(stored.getRole().getName()).isEqualTo("CUSTOMER");
        assertThat(stored.isActive()).isTrue();
        MockHttpSession session = login("  NEW-CUSTOMER@ONESHOP.TEST  ", "/");
        mvc.perform(get("/account/status").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isAdmin").value(false));
        mvc.perform(get("/admin/products/new").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/products").session(session).with(csrf())
                        .param("name", "Unauthorized product"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminLogsInThroughSameFormAndCanOpenProductCreation() throws Exception {
        account("admin@oneshop.test", "ADMIN", true);
        MockHttpSession session = login("admin@oneshop.test", "/admin");
        mvc.perform(get("/account/status").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isAdmin").value(true));
        mvc.perform(get("/admin/products/new").session(session)).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndDisabledAccountCannotAuthenticate() throws Exception {
        account("disabled@oneshop.test", "CUSTOMER", false);
        account("customer@oneshop.test", "CUSTOMER", true);
        for (String email : new String[]{"disabled@oneshop.test", "customer@oneshop.test"}) {
            mvc.perform(post("/login").with(csrf()).param("username", email)
                            .param("password", email.startsWith("disabled") ? PASSWORD : "wrong-password"))
                    .andExpect(redirectedUrl("/login?error"));
        }
    }

    @Test
    void loginReturnsCustomerToTheOrderPageTheyWereOpening() throws Exception {
        account("tracking@oneshop.test", "CUSTOMER", true);
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/orders").session(session)).andExpect(status().is3xxRedirection());
        mvc.perform(post("/login").session(session).with(csrf()).param("username","tracking@oneshop.test")
                .param("password",PASSWORD)).andExpect(redirectedUrl("/orders"));
    }

    @Test
    void logoutEndsTheAuthenticatedSession() throws Exception {
        account("customer@oneshop.test", "CUSTOMER", true);
        MockHttpSession session = login("customer@oneshop.test", "/");
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginRequiresCsrfToken() throws Exception {
        account("customer@oneshop.test", "CUSTOMER", true);
        mvc.perform(post("/login").param("username", "customer@oneshop.test").param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession login(String email, String destination) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                        .param("username", email).param("password", PASSWORD))
                .andExpect(redirectedUrl(destination)).andReturn().getRequest().getSession(false);
    }

    private void account(String email, String roleName, boolean active) {
        User user = new User();
        user.setFullName("OneShop test");
        user.setEmail(email);
        user.setPassword(encoder.encode(PASSWORD));
        user.setPhone("0912345678");
        user.setRole(role(roleName));
        user.setActive(active);
        users.save(user);
    }

    private Role role(String name) {
        return roles.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            return roles.save(role);
        });
    }
}
