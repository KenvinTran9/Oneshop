package com.oneshop.dto.request;

import com.oneshop.validation.BcryptPasswordLength;
import com.oneshop.validation.PasswordConfirmation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

@PasswordConfirmation
public record RegisterRequest(
        @NotBlank(message = "Vui lòng nhập họ và tên")
        @Size(max = 120, message = "Họ và tên không được vượt quá 120 ký tự")
        String fullName,

        @NotBlank(message = "Vui lòng nhập email")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 120, message = "Email không được vượt quá 120 ký tự")
        String email,

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(regexp = "^(?:0[35789]\\d{8}|\\+84[35789]\\d{8})$",
                message = "Số điện thoại phải có định dạng Việt Nam hợp lệ")
        @Size(max = 20, message = "Số điện thoại không hợp lệ")
        String phone,

        @NotBlank(message = "Vui lòng nhập mật khẩu")
        @Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự")
        @BcryptPasswordLength
        String password,

        @NotBlank(message = "Vui lòng xác nhận mật khẩu")
        String confirmPassword
) {
    public RegisterRequest {
        fullName = fullName == null ? null : fullName.trim();
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        phone = phone == null ? null : phone.trim();
    }

    @Override
    public String toString() {
        return "RegisterRequest[redacted]";
    }
}
