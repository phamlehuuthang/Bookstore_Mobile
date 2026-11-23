package com.trithucviet.bookstore.dto.request;

import com.trithucviet.bookstore.entity.enums.VaiTro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi Admin tạo tài khoản nhân sự mới (ADMIN / WAREHOUSE_MANAGER / CSKH) trên Web Admin.
 * Trường matKhau là tùy chọn; nếu để trống Backend sẽ cấp mật khẩu khởi tạo mặc định "123456".
 */
public record CreateStaffRequest(
        @NotBlank(message = "Họ tên nhân viên không được để trống.")
        @Size(max = 120, message = "Họ tên nhân viên tối đa 120 ký tự.")
        String hoTen,

        @NotBlank(message = "Email công vụ không được để trống.")
        @Email(message = "Email công vụ không đúng định dạng.")
        @Size(max = 120, message = "Email công vụ tối đa 120 ký tự.")
        String email,

        @NotBlank(message = "Số điện thoại không được để trống.")
        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
        String soDienThoai,

        @NotNull(message = "Vai trò nhân viên không được để trống.")
        VaiTro vaiTro,

        @Size(max = 100, message = "Mật khẩu tối đa 100 ký tự.")
        String matKhau
) {
}
