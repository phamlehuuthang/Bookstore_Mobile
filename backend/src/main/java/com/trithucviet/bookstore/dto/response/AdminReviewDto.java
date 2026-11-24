package com.trithucviet.bookstore.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông tin 1 đánh giá sách phục vụ màn hình Kiểm duyệt bình luận trên Web Admin (GET /api/admin/reviews).
 */
public record AdminReviewDto(
        String id,
        String sachId,
        String tieuDeSach,
        String nguoiDungId,
        String tenNguoiDung,
        String donHangId,
        Integer soSao,
        String binhLuan,
        List<String> anhDanhGiaUrls,
        Boolean biAn,
        LocalDateTime ngayTao
) {
}
