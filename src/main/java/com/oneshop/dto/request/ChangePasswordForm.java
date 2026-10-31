package com.oneshop.dto.request;
import jakarta.validation.constraints.*;
import com.oneshop.validation.BcryptPasswordLength;
public record ChangePasswordForm(@NotBlank @Size(max=200) String currentPassword,
        @NotBlank @Size(min=8,max=72) @BcryptPasswordLength String newPassword,@NotBlank String confirmation){
    @AssertTrue(message="Mật khẩu xác nhận chưa khớp.")
    public boolean isConfirmed(){return newPassword!=null && newPassword.equals(confirmation);}
}
