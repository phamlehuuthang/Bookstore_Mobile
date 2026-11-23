package com.trithucviet.bookstore.dto.response;

import java.math.BigDecimal;

/**
 * Thông tin từng đầu sách đã mua lưu cố định trong chi_tiet_don_hang.
 */
public record OrderItemDto(
        String id,
        String sachId,
        String tieuDeSach,
        String anhBiaUrl,
        String tenTacGia,
        BigDecimal donGia,
        Integer soLuong,
        BigDecimal thanhTien
) {
}
