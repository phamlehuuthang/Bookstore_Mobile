package com.trithucviet.bookstore.dto.response;

import java.math.BigDecimal;

/**
 * Thông tin tóm tắt 1 cuốn sách hiển thị trên thẻ sách (Trang chủ, Danh mục, Tìm kiếm, Sách cùng chủ đề).
 * - donGiaApDung: giá bán thực tế hiện tại (ưu tiên giaFlashSale khi dangFlashSale = true và giaFlashSale > 0).
 * - phanTramGiam: phần trăm tiết kiệm so với giaGoc để hiển thị nhãn "-25%" chuẩn app TMĐT.
 */
public record BookDto(
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
        String loaiBia,
        Integer namXuatBan,
        String anhBiaUrl,
        String tomTatNgan,
        Boolean banChay
) {
}
