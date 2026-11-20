package com.trithucviet.bookstore.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO trả về từng cuốn sách trong Tủ sách yêu thích của khách hàng.
 *
 * Dùng record DTO riêng thay vì trả trực tiếp entity Sach vì:
 * 1. Tránh lộ các cột nội bộ / dữ liệu nặng của bảng sach (như vector_ngu_nghia 768 chiều).
 * 2. Tránh lỗi vòng lặp JSON khi serialize các quan hệ JPA LAZY (TacGia, DanhMuc).
 * 3. Đóng gói sẵn tên tác giả, tên danh mục và mốc thời gian khách đã thả tim (ngayThem)
 *    để màn hình Yêu thích trên Mobile render ngay chỉ qua 1 lần gọi API.
 */
public record WishlistItemResponse(
        String sachId,
        String tieuDe,
        String tenTacGia,
        String tenDanhMuc,
        BigDecimal giaBan,
        BigDecimal giaGoc,
        Boolean dangFlashSale,
        BigDecimal giaFlashSale,
        Integer tonKho,
        BigDecimal diemDanhGiaTb,
        String anhBiaUrl,
        LocalDateTime ngayThem
) {
}
