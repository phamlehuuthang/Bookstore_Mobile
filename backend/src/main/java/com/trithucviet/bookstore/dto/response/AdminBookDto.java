package com.trithucviet.bookstore.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thông tin đầy đủ của 1 cuốn sách phục vụ màn hình Quản lý Sách trên Web Admin (Bước 3 - phần 3).
 * Bao gồm cả trạng thái hienThi, soTrang, moTaChiTiet, tuKhoa và ngày tạo.
 */
public record AdminBookDto(
        String id,
        String maIsbn,
        String tieuDe,
        String duongDan,
        String danhMucId,
        String tenDanhMuc,
        String tenDanhMucCon,
        String tacGiaId,
        String tenTacGia,
        String nhaXuatBanId,
        String tenNhaXuatBan,
        BigDecimal giaBan,
        BigDecimal giaGoc,
        Boolean dangFlashSale,
        BigDecimal giaFlashSale,
        BigDecimal donGiaApDung,
        int phanTramGiam,
        Integer tonKho,
        Integer daBan,
        BigDecimal diemDanhGiaTb,
        Integer soLuotDanhGia,
        Integer soTrang,
        Integer namXuatBan,
        String loaiBia,
        String anhBiaUrl,
        String tomTatNgan,
        String moTaChiTiet,
        String tuKhoa,
        Boolean banChay,
        Boolean hienThi,
        LocalDateTime ngayTao
) {
}
