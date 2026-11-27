package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Email không được để trống.")
        @Email(message = "Email không đúng định dạng.")
        String email,
        @NotBlank(message = "Vui lòng nhập mã xác thực OTP 6 chữ số.")
        String otp,
        @NotBlank(message = "Mật khẩu mới không được để trống.")
        @Size(min = 6, max = 100, message = "Mật khẩu mới phải từ 6 đến 100 ký tự.")
        String matKhauMoi
) {
}
