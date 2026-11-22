package com.trithucviet.bookstore.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông tin chi tiết đầy đủ của 1 cuốn sách cho trang Chi tiết sách (GET /api/v1/books/{id}),
 * kèm tối đa 6 cuốn sách cùng chủ đề để gợi ý đọc thêm.
 */
public record BookDetailDto(
        String id,
        String maIsbn,
        String tieuDe,
        String duongDan,
        String danhMucId,
        String tenDanhMuc,
        String tenDanhMucCon,
        String tacGiaId,
        String tenTacGia,
        String tieuSuTacGia,
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
        List<String> anhChiTietUrls,
        String tomTatNgan,
        String moTaChiTiet,
        List<String> tuKhoa,
        Boolean banChay,
        LocalDateTime ngayTao,
        List<BookDto> sachCungChuDe
) {
}
