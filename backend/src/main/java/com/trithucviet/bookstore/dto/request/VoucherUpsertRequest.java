package com.trithucviet.bookstore.dto.request;

import com.trithucviet.bookstore.entity.enums.LoaiGiam;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dữ liệu đầu vào khi Quản trị viên (ADMIN) tạo mới hoặc cập nhật Voucher trên Web Admin
 * (POST /api/admin/vouchers và PUT /api/admin/vouchers/{id}).
 *
 * Lưu ý các trường tùy chọn cho phép bỏ trống (null) để khớp với form tạo nhanh trên Vouchers.tsx:
 * - donToiThieu: nếu null -> mặc định 0đ.
 * - danhMucId: nếu null hoặc rỗng -> áp dụng cho toàn bộ cửa hàng.
 * - ngayBatDau: nếu null khi tạo mới -> mặc định hôm nay; nếu null khi sửa -> giữ nguyên ngày cũ.
 * - dangHoatDong: nếu null khi tạo mới -> mặc định true (đang kích hoạt).
 */
public record VoucherUpsertRequest(
        @NotBlank(message = "Mã voucher không được để trống.")
        @Size(max = 30, message = "Mã voucher tối đa 30 ký tự.")
        String maCode,

        @NotBlank(message = "Tiêu đề chương trình không được để trống.")
        @Size(max = 150, message = "Tiêu đề tối đa 150 ký tự.")
        String tieuDe,

        @Size(max = 500, message = "Mô tả tối đa 500 ký tự.")
        String moTa,

        @NotNull(message = "Loại giảm giá không được để trống.")
        LoaiGiam loaiGiam,

        @NotNull(message = "Giá trị giảm không được để trống.")
        @DecimalMin(value = "1", message = "Giá trị giảm phải lớn hơn 0.")
        BigDecimal giaTriGiam,

        @DecimalMin(value = "0", message = "Mức giảm tối đa không được âm.")
        BigDecimal giamToiDa,

        @DecimalMin(value = "0", message = "Đơn tối thiểu không được âm.")
        BigDecimal donToiThieu,

        String danhMucId,

        @NotNull(message = "Giới hạn lượt dùng không được để trống.")
        @Min(value = 1, message = "Giới hạn lượt dùng phải từ 1 trở lên.")
        Integer gioiHanLuot,

        LocalDate ngayBatDau,

        @NotNull(message = "Ngày kết thúc không được để trống.")
        LocalDate ngayKetThuc,

        Boolean dangHoatDong
) {
}
