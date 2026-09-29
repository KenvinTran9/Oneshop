package com.oneshop.service;

import com.oneshop.dto.request.LoginRequest;
import com.oneshop.dto.request.RefreshTokenRequest;
import com.oneshop.dto.response.CurrentUserResponse;
import com.oneshop.dto.response.TokenResponse;
import com.oneshop.security.JwtService;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 JwtService jwtService,
                                 UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        return issueTokens(request.email());
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        String username = jwtService.extractRefreshTokenUsername(request.refreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isRefreshTokenValid(request.refreshToken(), userDetails)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        return issueTokens(username);
    }

    public Optional<CurrentUserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        boolean isAdmin = authorities.contains("ROLE_ADMIN");

        return Optional.of(new CurrentUserResponse(true, authentication.getName(), isAdmin, authorities));
    }

    private TokenResponse issueTokens(String username) {
        return new TokenResponse(
                "Bearer",
                jwtService.generateAccessToken(username),
                jwtService.getAccessTokenExpirationMillis(),
                jwtService.generateRefreshToken(username),
                jwtService.getRefreshTokenExpirationMillis()
        );
    }
}
