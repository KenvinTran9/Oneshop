package com.oneshop.security;

import com.oneshop.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final int MINIMUM_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;

    public JwtService(JwtProperties properties) {
        this.signingKey = createSigningKey(properties.secret());
        this.issuer = requireIssuer(properties.issuer());
        this.accessTokenExpirationMillis = requirePositiveExpiration(
                properties.accessTokenExpirationMillis(),
                "JWT access-token expiration"
        );
        this.refreshTokenExpirationMillis = requirePositiveExpiration(
                properties.refreshTokenExpirationMillis(),
                "JWT refresh-token expiration"
        );

        if (refreshTokenExpirationMillis <= accessTokenExpirationMillis) {
            throw new IllegalStateException(
                    "JWT refresh-token expiration must be greater than access-token expiration"
            );
        }
    }

    public String generateAccessToken(String username) {
        return generateToken(username, ACCESS_TOKEN_TYPE, accessTokenExpirationMillis);
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, REFRESH_TOKEN_TYPE, refreshTokenExpirationMillis);
    }

    public String extractAccessTokenUsername(String token) {
        return extractClaims(token, ACCESS_TOKEN_TYPE).getSubject();
    }

    public String extractRefreshTokenUsername(String token) {
        return extractClaims(token, REFRESH_TOKEN_TYPE).getSubject();
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, ACCESS_TOKEN_TYPE);
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, REFRESH_TOKEN_TYPE);
    }

    public long getAccessTokenExpirationMillis() {
        return accessTokenExpirationMillis;
    }

    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpirationMillis;
    }

    private String generateToken(String username, String tokenType, long expirationMillis) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("JWT subject must not be blank");
        }

        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(signingKey)
                .compact();
    }

    private boolean isTokenValid(String token, UserDetails userDetails, String expectedTokenType) {
        Claims claims = extractClaims(token, expectedTokenType);
        return userDetails.isEnabled()
                && userDetails.isAccountNonExpired()
                && userDetails.isAccountNonLocked()
                && userDetails.isCredentialsNonExpired()
                && userDetails.getUsername().equals(claims.getSubject());
    }

    private Claims extractClaims(String token, String expectedTokenType) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .require(TOKEN_TYPE_CLAIM, expectedTokenType)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey createSigningKey(String encodedSecret) {
        if (encodedSecret == null
                || encodedSecret.isBlank()
                || isUnresolvedPlaceholder(encodedSecret)) {
            throw new IllegalStateException(
                    "JWT secret is required. Set the JWT_SECRET environment variable"
            );
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(encodedSecret);
        } catch (DecodingException exception) {
            throw new IllegalStateException(
                    "JWT secret must be a valid Base64-encoded value",
                    exception
            );
        }

        if (keyBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret is too weak. JWT_SECRET must decode to at least 32 random bytes (256 bits)"
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private boolean isUnresolvedPlaceholder(String value) {
        String trimmedValue = value.trim();
        return trimmedValue.startsWith("${") && trimmedValue.endsWith("}");
    }

    private String requireIssuer(String configuredIssuer) {
        if (configuredIssuer == null || configuredIssuer.isBlank()) {
            throw new IllegalStateException(
                    "JWT issuer is required. Set the JWT_ISSUER environment variable"
            );
        }
        return configuredIssuer;
    }

    private long requirePositiveExpiration(long expirationMillis, String propertyName) {
        if (expirationMillis <= 0) {
            throw new IllegalStateException(propertyName + " must be greater than zero");
        }
        return expirationMillis;
    }
}
