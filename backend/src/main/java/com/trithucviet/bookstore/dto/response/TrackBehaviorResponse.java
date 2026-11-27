package com.trithucviet.bookstore.dto.response;

import com.trithucviet.bookstore.entity.enums.LoaiHanhVi;
import java.time.LocalDateTime;

/**
 * Kết quả ghi nhận sự kiện hành vi người dùng (Bước 8 Người A).
 * - ignored = true khi khách mở trang chi tiết sách nhưng thoát ra dưới 3 giây (Quy tắc 7.1).
 */
public record TrackBehaviorResponse(
        String id,
        String nguoiDungId,
        String sachId,
        String danhMucId,
        String tenDanhMuc,
        LoaiHanhVi loaiHanhVi,
        Integer thoiGianXemGiay,
        Integer trongSoDiem,
        boolean ignored,
        LocalDateTime ngayTao
) {
}
