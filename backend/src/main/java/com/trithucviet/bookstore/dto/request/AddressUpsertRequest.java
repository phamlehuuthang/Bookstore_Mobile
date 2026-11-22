package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi khách hàng thêm mới hoặc cập nhật 1 địa chỉ nhận hàng (POST/PUT /api/v1/addresses).
 */
public record AddressUpsertRequest(
        @NotBlank(message = "Họ tên người nhận không được để trống.")
        @Size(max = 120, message = "Họ tên người nhận tối đa 120 ký tự.")
        String hoTenNguoiNhan,

        @NotBlank(message = "Số điện thoại người nhận không được để trống.")
        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
        @Pattern(regexp = "^[0-9+\\s.-]{8,20}$", message = "Số điện thoại người nhận không hợp lệ.")
        String soDienThoai,

        @NotBlank(message = "Tỉnh/Thành phố không được để trống.")
        @Size(max = 100, message = "Tỉnh/Thành phố tối đa 100 ký tự.")
        String tinhThanh,

        @NotBlank(message = "Quận/Huyện không được để trống.")
        @Size(max = 100, message = "Quận/Huyện tối đa 100 ký tự.")
        String quanHuyen,

        @NotBlank(message = "Phường/Xã không được để trống.")
        @Size(max = 100, message = "Phường/Xã tối đa 100 ký tự.")
        String phuongXa,

        @NotBlank(message = "Địa chỉ chi tiết không được để trống.")
        @Size(max = 255, message = "Địa chỉ chi tiết tối đa 255 ký tự.")
        String diaChiChiTiet,

        // Có thể null: khi thêm địa chỉ đầu tiên sẽ tự động bật true; khi cập nhật nếu null thì giữ nguyên trạng thái cũ
        Boolean macDinh
) {
}
