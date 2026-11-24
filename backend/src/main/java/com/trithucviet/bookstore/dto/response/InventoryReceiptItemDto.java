package com.trithucviet.bookstore.dto.response;

import java.math.BigDecimal;

/**
 * Chi tiết 1 dòng đầu sách trong Phiếu nhập kho.
 */
public record InventoryReceiptItemDto(
        String id,
        String sachId,
        String tieuDeSach,
        String maIsbn,
        Integer soLuong,
        BigDecimal giaNhap,
        BigDecimal thanhTien
) {
}
