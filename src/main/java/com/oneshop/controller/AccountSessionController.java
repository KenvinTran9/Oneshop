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
    public AccountSessionController(AuthenticationService authentication) { this.authentication = authentication; }

    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication current) {
        return authentication.getCurrentUser(current)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body(new AuthenticationStatusResponse(false)));
    }

}
