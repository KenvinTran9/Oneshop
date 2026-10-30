package com.oneshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.oneshop.validation.BcryptPasswordLength;

public record LoginRequest(
        @Email @NotBlank @Size(max = 120) String email,
        @NotBlank @Size(max = 72) @BcryptPasswordLength String password
) {
    public LoginRequest {
        email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
    @Override public String toString() { return "LoginRequest[redacted]"; }
}
