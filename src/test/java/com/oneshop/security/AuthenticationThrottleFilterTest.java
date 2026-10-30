package com.oneshop.security;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthenticationThrottleFilterTest {
    @Test void webAndApiLoginsShareBudgetAndSpoofedHeadersCannotResetIt() throws Exception {
        var filter = new AuthenticationThrottleFilter(true);
        for (int i = 0; i < 15; i++) assertThat(call(filter, "/login", "127.0.0.1", "client-" + i).getStatus()).isEqualTo(204);
        var blocked = call(filter, "/api/auth/login", "127.0.0.1", "new-spoofed-ip");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(Integer.parseInt(blocked.getHeader("Retry-After"))).isBetween(1, 60);
        assertThat(blocked.getContentType()).startsWith("application/json");
        assertThat(call(filter, "/login", "127.0.0.2", "127.0.0.1").getStatus()).isEqualTo(204);
        assertThat(call(filter, "/api/auth/refresh", "127.0.0.1", "").getStatus()).isEqualTo(204);
    }

    @Test void registrationHasSeparateSmallerBudgetAndNormalBrowsingIsUnaffected() throws Exception {
        var filter = new AuthenticationThrottleFilter(true);
        for (int i = 0; i < 5; i++) assertThat(call(filter, "/register", "client", "").getStatus()).isEqualTo(204);
        assertThat(call(filter, "/register", "client", "").getStatus()).isEqualTo(429);
        assertThat(call(filter, "/login", "client", "").getStatus()).isEqualTo(204);
        assertThat(call(filter, "/cart/items", "client", "").getStatus()).isEqualTo(204);
    }

    @Test void budgetExpiresWithoutSleeping() throws Exception {
        var clock = new MutableClock();
        var filter = new AuthenticationThrottleFilter(true, clock);
        for (int i = 0; i < 30; i++) call(filter, "/api/auth/refresh", "client", "");
        assertThat(call(filter, "/api/auth/refresh", "client", "").getStatus()).isEqualTo(429);
        clock.now += 60_000;
        assertThat(call(filter, "/api/auth/refresh", "client", "").getStatus()).isEqualTo(204);
    }

    @Test void contextPathCannotBypassThrottling() throws Exception {
        var filter = new AuthenticationThrottleFilter(true);
        for (int i = 0; i < 16; i++) {
            var request = new MockHttpServletRequest("POST", "/shop/login");
            request.setContextPath("/shop");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(204));
            assertThat(response.getStatus()).isEqualTo(i < 15 ? 204 : 429);
        }
    }

    private MockHttpServletResponse call(AuthenticationThrottleFilter filter, String path, String address, String forwarded)
            throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(address);
        request.addHeader("X-Forwarded-For", forwarded);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(204));
        return response;
    }

    private static class MutableClock extends Clock {
        long now = 1_000_000;
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(now); }
    }
}
