package com.trithucviet.bookstore.dto.response;

import com.trithucviet.bookstore.entity.enums.LoaiGiam;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Thông tin chi tiết 1 mã giảm giá trả về cho Mobile App.
 *
 * Lưu ý trường danhMucId / tenDanhMucApDung:
 * - Nếu = null: mã áp dụng cho toàn bộ sách trong cửa hàng.
 * - Nếu != null: mã chỉ áp dụng cho sách thuộc danh mục đó (hoặc các danh mục con của nó).
 */
public record VoucherDto(
        String id,
        String maCode,
        String tieuDe,
        String moTa,
        LoaiGiam loaiGiam,
        BigDecimal giaTriGiam,
        BigDecimal giamToiDa,
        BigDecimal donToiThieu,
        Integer gioiHanLuot,
        Integer daSuDung,
        int conLaiLuot,
        LocalDate ngayBatDau,
        LocalDate ngayKetThuc,
        Boolean dangHoatDong,
        String danhMucId,
        String tenDanhMucApDung
) {
}
