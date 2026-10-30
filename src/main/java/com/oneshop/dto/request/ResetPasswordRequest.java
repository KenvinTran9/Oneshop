package com.oneshop.dto.request;

import com.oneshop.validation.BcryptPasswordLength;
import jakarta.validation.constraints.*;

public record ResetPasswordRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
        @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
        @Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự.")
        @BcryptPasswordLength String password,
        @NotBlank(message = "Vui lòng xác nhận mật khẩu mới.") String confirmPassword) {
    @Override public String toString() { return "ResetPasswordRequest[redacted]"; }
}
