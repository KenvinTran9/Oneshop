package com.oneshop.dto.response;

import java.util.List;

public record CurrentUserResponse(
        boolean authenticated,
        String email,
        boolean isAdmin,
        List<String> authorities
) {
}
