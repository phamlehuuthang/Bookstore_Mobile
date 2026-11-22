package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi khách hàng đăng ký tài khoản mới trên Mobile App (POST /api/v1/auth/register).
 */
public record RegisterRequest(
        @NotBlank(message = "Họ tên không được để trống.")
        @Size(max = 120, message = "Họ tên tối đa 120 ký tự.")
        String hoTen,

        @NotBlank(message = "Email không được để trống.")
        @Email(message = "Email không đúng định dạng.")
        @Size(max = 120, message = "Email tối đa 120 ký tự.")
        String email,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
        @Pattern(regexp = "^$|^[0-9+\\s.-]{8,20}$", message = "Số điện thoại không hợp lệ.")
        String soDienThoai,

        @NotBlank(message = "Mật khẩu không được để trống.")
        @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự.")
        String matKhau
) {
}
