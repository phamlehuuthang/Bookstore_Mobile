package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi Admin / Quản lý Kho thêm hoặc cập nhật Nhà xuất bản (Bước 3 - phần 4).
 */
public record PublisherUpsertRequest(
        @NotBlank(message = "Tên nhà xuất bản không được để trống.")
        @Size(max = 150, message = "Tên nhà xuất bản tối đa 150 ký tự.")
        String tenNxb,

        @Size(max = 255, message = "Địa chỉ nhà xuất bản tối đa 255 ký tự.")
        String diaChi
) {
}
