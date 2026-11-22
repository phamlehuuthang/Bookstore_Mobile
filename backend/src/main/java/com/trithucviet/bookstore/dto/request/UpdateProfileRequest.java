package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi người dùng cập nhật thông tin hồ sơ cá nhân (PUT /api/v1/auth/profile).
 * Không cho phép tự ý sửa email, vai trò hay trạng thái khóa từ màn hình hồ sơ.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Họ tên không được để trống.")
        @Size(max = 120, message = "Họ tên tối đa 120 ký tự.")
        String hoTen,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
        @Pattern(regexp = "^$|^[0-9+\\s.-]{8,20}$", message = "Số điện thoại không hợp lệ.")
        String soDienThoai
) {
}
