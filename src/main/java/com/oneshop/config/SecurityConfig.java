package com.oneshop.config;

import com.oneshop.security.AuthenticationThrottleFilter;
import com.oneshop.security.CartLoginSuccessHandler;
import com.oneshop.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtAuthenticationFilter jwt,
                                   AuthenticationThrottleFilter throttle) throws Exception {
        // API credentials must be explicit: browser session cookies never authenticate this chain.
        http.securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, failure) ->
                                jsonError(response, 401, "Vui lòng xác thực bằng access token hợp lệ."))
                        .accessDeniedHandler((request, response, failure) ->
                                jsonError(response, 403, "Bạn không có quyền thực hiện thao tác này.")))
                .addFilterBefore(throttle, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
        securityHeaders(http);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain webSecurity(HttpSecurity http, CartLoginSuccessHandler success,
                                   AuthenticationThrottleFilter throttle, com.oneshop.security.CredentialVersionFilter credentials) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                        .requestMatchers("/", "/healthz", "/products/**", "/stores/**", "/cart", "/cart/**",
                                "/login", "/register", "/forgot-password", "/reset-password", "/account/status", "/css/**", "/js/**",
                                "/images/**", "/decorators/**", "/webjars/**").permitAll()
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(login -> login.loginPage("/login").successHandler(success).permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout").deleteCookies("JSESSIONID").permitAll())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                .addFilterBefore(throttle, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(credentials, UsernamePasswordAuthenticationFilter.class);
        securityHeaders(http);
        return http.build();
    }

    private void securityHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(policy -> policy.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(policy -> policy.policyDirectives(
                        "default-src 'self'; script-src 'self' https://cdn.jsdelivr.net; "
                        + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; "
                        + "font-src 'self' https://cdn.jsdelivr.net https://fonts.gstatic.com data:; "
                        + "img-src 'self' https: data:; connect-src 'self'; "
                        + "frame-src https://www.google.com https://maps.google.com; "
                        + "object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'"))
                .permissionsPolicyHeader(policy -> policy.policy("camera=(), microphone=(), geolocation=()"))
                .addHeaderWriter((request, response) -> {
                    if ((request.getContextPath() + "/reset-password").equals(request.getRequestURI()))
                        response.setHeader("Referrer-Policy", "no-referrer");
                }));
    }

    private static void jsonError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"authenticated\":false,\"message\":\"" + message + "\"}");
    }

    // Run filters only inside their selected security chain, not as container-wide servlet filters.
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtRegistration(JwtAuthenticationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<AuthenticationThrottleFilter> throttleRegistration(AuthenticationThrottleFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
    @Bean
    FilterRegistrationBean<com.oneshop.security.CredentialVersionFilter> credentialsRegistration(com.oneshop.security.CredentialVersionFilter filter) {
        var registration = new FilterRegistrationBean<>(filter); registration.setEnabled(false); return registration;
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
