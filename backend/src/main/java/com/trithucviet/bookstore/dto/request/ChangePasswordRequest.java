package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi người dùng đổi mật khẩu (PUT /api/v1/auth/password).
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại.")
        String matKhauCu,

        @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
        @Size(min = 6, max = 100, message = "Mật khẩu mới phải từ 6 đến 100 ký tự.")
        String matKhauMoi
) {
}
