package com.oneshop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded, per-instance request budget. Never trusts client-supplied forwarding headers. */
@Component
public class AuthenticationThrottleFilter extends OncePerRequestFilter {
    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_BUCKETS = 10_000;
    private final Map<String, Bucket> buckets = new HashMap<>();
    private final Clock clock;
    private final boolean enabled;

    @Autowired
    public AuthenticationThrottleFilter(@Value("${app.security.auth-rate-limit.enabled:true}") boolean enabled) {
        this(enabled, Clock.systemUTC());
    }

    AuthenticationThrottleFilter(boolean enabled, Clock clock) { this.enabled = enabled; this.clock = clock; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        int limit = limit(request);
        if (!enabled || limit == 0) { chain.doFilter(request, response); return; }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String group = path.contains("refresh") ? "refresh" : path.equals("/register") ? "register"
                : path.equals("/forgot-password") ? "forgot-password" : path.equals("/reset-password") ? "reset-password" : "login";
        long retry = reserve(request.getRemoteAddr() + ":" + group, limit);
        if (retry == 0) { chain.doFilter(request, response); return; }
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retry));
        response.setHeader("Cache-Control", "no-store");
        String message = "Bạn thao tác quá nhanh. Vui lòng đợi " + retry + " giây rồi thử lại.";
        boolean api = path.startsWith("/api/");
        response.setContentType(api ? "application/json;charset=UTF-8" : "text/plain;charset=UTF-8");
        response.getWriter().write(api ? "{\"message\":\"" + message + "\"}" : message);
    }

    private int limit(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return 0;
        return switch (request.getRequestURI().substring(request.getContextPath().length())) {
            case "/login", "/api/auth/login" -> 15;
            case "/register" -> 5;
            case "/api/auth/refresh" -> 30;
            case "/forgot-password" -> 5;
            case "/reset-password" -> 10;
            default -> 0;
        };
    }

    private synchronized long reserve(String key, int limit) {
        long now = clock.millis();
        Bucket bucket = buckets.get(key);
        if (bucket == null || now >= bucket.expiresAt) {
            buckets.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt);
            if (buckets.size() >= MAX_BUCKETS) return 60;
            buckets.put(key, new Bucket(now + WINDOW_MILLIS));
            return 0;
        }
        if (bucket.requests >= limit) return Math.max(1, (bucket.expiresAt - now + 999) / 1000);
        bucket.requests++;
        return 0;
    }

    private static final class Bucket {
        final long expiresAt;
        int requests = 1;
        Bucket(long expiresAt) { this.expiresAt = expiresAt; }
    }
}
