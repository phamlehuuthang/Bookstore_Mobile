package com.trithucviet.bookstore.dto.response;

/**
 * Thông tin danh mục phẳng (gồm cả danh mục đang tắt dangHoatDong = false) phục vụ Web Admin (Bước 3 - phần 4).
 */
public record AdminCategoryDto(
        String id,
        String tenDanhMuc,
        String duongDan,
        String danhMucChaId,
        String moTa,
        String tenBieuTuong,
        Integer thuTuHienThi,
        Boolean dangHoatDong,
        long soLuongSach
) {
}
