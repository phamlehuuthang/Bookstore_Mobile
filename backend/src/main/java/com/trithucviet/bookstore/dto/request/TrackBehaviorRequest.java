package com.trithucviet.bookstore.dto.request;

import com.trithucviet.bookstore.entity.enums.LoaiHanhVi;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payload gửi sự kiện theo dõi hành vi người dùng từ Mobile App (Bước 8 Người A).
 * - Nếu truyền sachId: Backend tự lấy danhMucId chuẩn từ cuốn sách đó.
 * - Nếu không truyền sachId (ví dụ TIM_KIEM hoặc BO_QUA_DE_XUAT theo chủ đề): truyền danhMucId.
 */
public record TrackBehaviorRequest(
        @NotNull(message = "Loại hành vi không được để trống")
        LoaiHanhVi loaiHanhVi,

        String sachId,

        String danhMucId,

        @Min(value = 0, message = "Thời gian xem (giây) không được âm")
        Integer thoiGianXemGiay
) {
}
