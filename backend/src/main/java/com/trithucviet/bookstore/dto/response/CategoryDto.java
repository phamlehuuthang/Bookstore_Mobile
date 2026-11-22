package com.trithucviet.bookstore.dto.response;

import java.util.List;

/**
 * Thông tin 1 danh mục sách theo cấu trúc cây Cha - Con (GET /api/v1/categories).
 * Với danh mục cha, soLuongSach đã được cộng gộp cả sách nằm trong các danh mục con đang hoạt động.
 */
public record CategoryDto(
        String id,
        String tenDanhMuc,
        String duongDan,
        String danhMucChaId,
        String moTa,
        String tenBieuTuong,
        Integer thuTuHienThi,
        long soLuongSach,
        List<CategoryDto> danhMucCon
) {
}
