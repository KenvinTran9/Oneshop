package com.oneshop.api.controller;

import com.oneshop.dto.request.LoginRequest;
import com.oneshop.dto.request.RefreshTokenRequest;
import com.oneshop.dto.response.AuthenticationStatusResponse;
import com.oneshop.dto.response.CurrentUserResponse;
import com.oneshop.dto.response.TokenResponse;
import com.oneshop.service.AuthenticationService;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationService authenticationService;

    public AuthApiController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authenticationService.refresh(request);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        Optional<CurrentUserResponse> currentUser = authenticationService.getCurrentUser(authentication);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthenticationStatusResponse(false));
        }
        return ResponseEntity.ok(currentUser.orElseThrow());
    }
}
