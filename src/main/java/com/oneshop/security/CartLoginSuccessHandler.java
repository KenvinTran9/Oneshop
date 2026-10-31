package com.oneshop.security;

import com.oneshop.service.CartService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Component;

@Component
public class CartLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final CartService cartService;

    public CartLoginSuccessHandler(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        var session = request.getSession(false);
        if (session != null) {
            cartService.mergeGuestCart(authentication.getName(), session);
            session.removeAttribute(CartService.GUEST_CART_SESSION_ATTRIBUTE);
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        var cache = new org.springframework.security.web.savedrequest.HttpSessionRequestCache();
        var saved = cache.getRequest(request, response);
        cache.removeRequest(request, response);
        if (saved != null && "GET".equals(saved.getMethod())) {
            String path = java.net.URI.create(saved.getRedirectUrl()).getPath();
            String local = path.substring(Math.min(request.getContextPath().length(), path.length()));
            if (local.equals("/checkout") || local.equals("/orders") || local.matches("/orders/OS-[A-F0-9]{16}")) {
                response.sendRedirect(request.getContextPath() + local); return;
            }
        }
        boolean manager = authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_BRANCH_MANAGER".equals(authority.getAuthority()));
        response.sendRedirect(request.getContextPath() + (admin ? "/admin" : manager ? "/branch" : "/"));
    }
}
