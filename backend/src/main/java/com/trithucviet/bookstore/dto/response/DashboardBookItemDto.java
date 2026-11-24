package com.trithucviet.bookstore.dto.response;

import java.math.BigDecimal;

/**
 * Dữ liệu sách rút gọn dùng cho Top sách bán chạy & Cảnh báo tồn kho trên Dashboard / Inventory.
 */
public record DashboardBookItemDto(
        String id,
        String maIsbn,
        String tieuDe,
        String tenTacGia,
        String tenDanhMuc,
        String tenNhaXuatBan,
        String anhBiaUrl,
        BigDecimal giaBan,
        BigDecimal giaGoc,
        Boolean dangFlashSale,
        BigDecimal giaFlashSale,
        Integer tonKho,
        Integer daBan
) {
}
