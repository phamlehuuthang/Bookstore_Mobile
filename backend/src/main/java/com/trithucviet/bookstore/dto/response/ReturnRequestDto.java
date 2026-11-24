package com.trithucviet.bookstore.dto.response;

import com.trithucviet.bookstore.entity.enums.TrangThaiDoiTra;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông tin chi tiết yêu cầu Đổi/Trả hàng gắn với 1 đơn hàng (quan hệ 1-1).
 */
public record ReturnRequestDto(
        String id,
        String donHangId,
        String maDonHang,
        String tenKhachHang,
        String lyDo,
        String moTa,
        List<String> anhMinhChungUrls,
        TrangThaiDoiTra trangThai,
        String ghiChuAdmin,
        LocalDateTime ngayTao,
        LocalDateTime ngayXuLy
) {
}
