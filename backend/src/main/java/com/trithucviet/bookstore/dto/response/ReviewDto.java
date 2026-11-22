package com.trithucviet.bookstore.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông tin 1 đánh giá công khai của độc giả hiển thị tại trang Chi tiết sách (GET /api/v1/books/{id}/reviews).
 */
public record ReviewDto(
        String id,
        String sachId,
        String nguoiDungId,
        String tenNguoiDung,
        Integer soSao,
        String binhLuan,
        List<String> anhDanhGiaUrls,
        LocalDateTime ngayTao
) {
}
