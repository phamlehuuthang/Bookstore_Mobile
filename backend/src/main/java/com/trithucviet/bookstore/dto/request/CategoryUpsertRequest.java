package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi Admin / Quản lý Kho tạo mới hoặc cập nhật Danh mục sách Cha/Con (Bước 3 - phần 4).
 */
public record CategoryUpsertRequest(
        @NotBlank(message = "Tên danh mục không được để trống.")
        @Size(max = 120, message = "Tên danh mục tối đa 120 ký tự.")
        String tenDanhMuc,

        String danhMucChaId,

        @Size(max = 500, message = "Mô tả danh mục tối đa 500 ký tự.")
        String moTa,

        @Size(max = 50, message = "Tên biểu tượng tối đa 50 ký tự.")
        String tenBieuTuong,

        Integer thuTuHienThi,
        Boolean dangHoatDong
) {
}
