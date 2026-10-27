package com.oneshop.security;

import com.oneshop.service.CartService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
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
        }
        response.sendRedirect(request.getContextPath() + "/");
    }
}
