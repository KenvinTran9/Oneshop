package com.oneshop.dto.response;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresInMillis,
        String refreshToken,
        long refreshTokenExpiresInMillis
) {
    @Override public String toString() { return "TokenResponse[redacted]"; }
}
