package com.oneshop.security;

import com.oneshop.repository.PasswordResetAccountRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CredentialVersionFilter extends OncePerRequestFilter {
    private final PasswordResetAccountRepository accounts;
    public CredentialVersionFilter(PasswordResetAccountRepository accounts) { this.accounts = accounts; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)
                && AccountUserDetails.versionOf(authentication.getPrincipal()) != accounts.findCredentialVersion(authentication.getName()).orElse(0L)) {
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }
}
