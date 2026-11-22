package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Dữ liệu đầu vào cho API Đăng nhập (dùng chung cho cả Mobile App và Web Admin).
 */
public record LoginRequest(
        @NotBlank(message = "Email không được để trống.")
        @Email(message = "Email không đúng định dạng.")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống.")
        String matKhau
) {
}
