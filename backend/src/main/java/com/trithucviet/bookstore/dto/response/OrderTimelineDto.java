package com.trithucviet.bookstore.dto.response;

/**
 * Một mốc sự kiện trong hành trình vận chuyển đơn hàng (lich_su_don_hang).
 */
public record OrderTimelineDto(
        String id,
        String trangThai,
        String tieuDe,
        String moTa,
        String thoiGian
) {
}
