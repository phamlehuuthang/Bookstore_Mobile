package com.trithucviet.bookstore.dto.response;

import com.trithucviet.bookstore.entity.enums.LoaiThongBao;
import java.time.LocalDateTime;

/**
 * Thông tin 1 bản ghi thông báo trả về cho Mobile App & Web Admin (Bước 7 - Phần 3).
 */
public record NotificationDto(
        String id,
        String nguoiDungId,
        String tieuDe,
        String noiDung,
        LoaiThongBao loaiThongBao,
        Boolean daDoc,
        LocalDateTime ngayTao
) {
}
