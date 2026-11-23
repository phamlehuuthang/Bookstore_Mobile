package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Dữ liệu đầu vào khi Admin / Quản lý Kho thêm mới hoặc cập nhật thông tin đầu sách (Bước 3 - phần 3).
 */
public record BookUpsertRequest(
        @Size(max = 30, message = "Mã ISBN tối đa 30 ký tự.")
        String maIsbn,

        @NotBlank(message = "Tên cuốn sách không được để trống.")
        @Size(max = 255, message = "Tên cuốn sách tối đa 255 ký tự.")
        String tieuDe,

        @NotBlank(message = "Danh mục sách không được để trống.")
        String danhMucId,

        @Size(max = 120, message = "Tên danh mục con tối đa 120 ký tự.")
        String tenDanhMucCon,

        String tacGiaId,

        @Size(max = 150, message = "Tên tác giả tối đa 150 ký tự.")
        String tenTacGia,

        String nhaXuatBanId,

        @Size(max = 150, message = "Tên nhà xuất bản tối đa 150 ký tự.")
        String tenNhaXuatBan,

        BigDecimal giaBan,
        BigDecimal giaGoc,
        Boolean dangFlashSale,
        BigDecimal giaFlashSale,
        Integer tonKho,
        Integer soTrang,
        Integer namXuatBan,

        @Size(max = 30, message = "Loại bìa tối đa 30 ký tự.")
        String loaiBia,

        @Size(max = 500, message = "Đường dẫn ảnh bìa tối đa 500 ký tự.")
        String anhBiaUrl,

        @Size(max = 500, message = "Tóm tắt ngắn tối đa 500 ký tự.")
        String tomTatNgan,

        String moTaChiTiet,

        @Size(max = 500, message = "Từ khóa tối đa 500 ký tự.")
        String tuKhoa,

        Boolean banChay,
        Boolean hienThi
) {
}
