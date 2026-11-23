package com.trithucviet.bookstore.dto.response;

import com.trithucviet.bookstore.entity.enums.PhuongThucThanhToan;
import com.trithucviet.bookstore.entity.enums.TrangThaiDon;
import com.trithucviet.bookstore.entity.enums.TrangThaiThanhToan;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu đầy đủ của 1 đơn hàng trả về cho cả Mobile App và Web Admin (Bước 6).
 */
public record OrderDto(
        String id,
        String maDonHang,
        String nguoiDungId,
        String tenKhachHang,
        String sdtKhachHang,
        String diaChiGiaoHang,
        BigDecimal tamTinh,
        BigDecimal phiVanChuyen,
        BigDecimal tienGiamGia,
        String maGiamGiaCode,
        BigDecimal tongThanhToan,
        PhuongThucThanhToan phuongThucThanhToan,
        TrangThaiThanhToan trangThaiThanhToan,
        TrangThaiDon trangThaiDon,
        String ghiChu,
        Boolean daDanhGia,
        LocalDateTime ngayTao,
        List<OrderItemDto> items,
        List<OrderTimelineDto> timeline
) {
}
