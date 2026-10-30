package com.oneshop.controller;

import com.oneshop.dto.response.AuthenticationStatusResponse;
import com.oneshop.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cookie-authenticated helpers are separate from the Bearer-only API. */
@RestController
@RequestMapping("/account")
public class AccountSessionController {
    private final AuthenticationService authentication;
    private final com.oneshop.service.CartService carts;
    public AccountSessionController(AuthenticationService authentication, com.oneshop.service.CartService carts) {
        this.authentication = authentication; this.carts = carts;
    }

    @GetMapping("/header")
    public com.oneshop.dto.response.HeaderStatusResponse header(Authentication current, jakarta.servlet.http.HttpServletRequest request) {
        var account = authentication.getCurrentUser(current);
        String email = account.map(user -> user.email()).orElse(null);
        return new com.oneshop.dto.response.HeaderStatusResponse(account.isPresent(), email,
                account.map(user -> user.isAdmin()).orElse(false), carts.getItemCount(email, request.getSession(false)));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication current) {
        return authentication.getCurrentUser(current)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body(new AuthenticationStatusResponse(false)));
    }

}
