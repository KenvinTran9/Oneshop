package com.oneshop.dto.request;

import jakarta.validation.constraints.*;
import java.util.Locale;

public record ForgotPasswordRequest(
        @NotBlank(message = "Vui lòng nhập email đã đăng ký.")
        @Email(message = "Email không đúng định dạng.")
        @Size(max = 120, message = "Email tối đa 120 ký tự.") String email) {
    public ForgotPasswordRequest { email = email == null ? null : email.trim().toLowerCase(Locale.ROOT); }
    @Override public String toString() { return "ForgotPasswordRequest[redacted]"; }
}
