package com.oneshop.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oneshop.config.JwtProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private static final String USERNAME = "user@oneshop.vn";
    private static final String ISSUER = "oneshop-test";
    private static final String VALID_SECRET = encode("0123456789abcdef0123456789abcdef");
    private static final String OTHER_SECRET = encode("fedcba9876543210fedcba9876543210");
    private static final long ACCESS_EXPIRATION = 900_000L;
    private static final long REFRESH_EXPIRATION = 604_800_000L;

    @Test
    void validAccessTokenIsAccepted() {
        JwtService jwtService = jwtService(VALID_SECRET, ISSUER);
        UserDetails userDetails = userDetails();

        String token = jwtService.generateAccessToken(USERNAME);

        assertThat(jwtService.extractAccessTokenUsername(token)).isEqualTo(USERNAME);
        assertThat(jwtService.isAccessTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwtService = jwtService(VALID_SECRET, ISSUER);
        String token = signedToken(
                VALID_SECRET,
                ISSUER,
                "access",
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(60)
        );

        assertThatThrownBy(() -> jwtService.extractAccessTokenUsername(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenWithWrongSignatureIsRejected() {
        JwtService jwtService = jwtService(VALID_SECRET, ISSUER);
        String token = jwtService(OTHER_SECRET, ISSUER).generateAccessToken(USERNAME);

        assertThatThrownBy(() -> jwtService.extractAccessTokenUsername(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithWrongIssuerIsRejected() {
        JwtService jwtService = jwtService(VALID_SECRET, ISSUER);
        String token = signedToken(
                VALID_SECRET,
                "another-issuer",
                "access",
                Instant.now(),
                Instant.now().plusSeconds(60)
        );

        assertThatThrownBy(() -> jwtService.extractAccessTokenUsername(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void missingOrWeakSecretFailsFast() {
        assertThatThrownBy(() -> jwtService(null, ISSUER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");

        assertThatThrownBy(() -> jwtService("${JWT_SECRET}", ISSUER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");

        assertThatThrownBy(() -> jwtService(encode("too-short"), ISSUER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 random bytes");
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        JwtService jwtService = jwtService(VALID_SECRET, ISSUER);
        String refreshToken = jwtService.generateRefreshToken(USERNAME);

        assertThat(jwtService.extractRefreshTokenUsername(refreshToken)).isEqualTo(USERNAME);
        assertThatThrownBy(() -> jwtService.extractAccessTokenUsername(refreshToken))
                .isInstanceOf(JwtException.class);
    }

    private static JwtService jwtService(String secret, String issuer) {
        return new JwtService(new JwtProperties(
                secret,
                issuer,
                ACCESS_EXPIRATION,
                REFRESH_EXPIRATION
        ));
    }

    private static UserDetails userDetails() {
        return User.withUsername(USERNAME)
                .password("not-used")
                .authorities("ROLE_CUSTOMER")
                .build();
    }

    private static String signedToken(String secret,
                                      String issuer,
                                      String tokenType,
                                      Instant issuedAt,
                                      Instant expiresAt) {
        return Jwts.builder()
                .subject(USERNAME)
                .issuer(issuer)
                .claim("token_type", tokenType)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .compact();
    }

    private static String encode(String value) {
        return Encoders.BASE64.encode(value.getBytes(StandardCharsets.UTF_8));
    }
}
