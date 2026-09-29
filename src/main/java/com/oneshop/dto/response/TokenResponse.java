package com.oneshop.dto.response;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresInMillis,
        String refreshToken,
        long refreshTokenExpiresInMillis
) {
}
