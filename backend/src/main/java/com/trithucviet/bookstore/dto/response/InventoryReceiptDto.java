package com.trithucviet.bookstore.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu 1 Phiếu Nhập Kho đầy đủ trả về cho Web Admin (Bước 8).
 */
public record InventoryReceiptDto(
        String id,
        String maPhieu,
        String tenNhaCungCap,
        String nguoiTao,
        String ghiChu,
        Integer tongSoLuong,
        BigDecimal tongTienNhap,
        LocalDateTime ngayTao,
        List<InventoryReceiptItemDto> items
) {
}
