package com.trithucviet.bookstore.dto.request;

import com.trithucviet.bookstore.entity.enums.LoaiThongBao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi ADMIN / CSKH phát sóng thông báo khuyến mãi hoặc hệ thống tới toàn bộ khách hàng.
 */
public record CreateNotificationRequest(
        @NotBlank(message = "Tiêu đề thông báo không được để trống.")
        @Size(max = 200, message = "Tiêu đề thông báo tối đa 200 ký tự.")
        String tieuDe,

        @NotBlank(message = "Nội dung thông báo không được để trống.")
        @Size(max = 1000, message = "Nội dung thông báo tối đa 1000 ký tự.")
        String noiDung,

        LoaiThongBao loaiThongBao
) {
}
