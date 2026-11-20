package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * Dữ liệu đầu vào khi khách hàng kiểm tra & áp thử mã giảm giá (POST /api/v1/vouchers/validate).
 *
 * - maCode: bắt buộc nhập (server tự cắt khoảng trắng đầu/cuối và không phân biệt hoa/thường).
 * - tamTinh: tùy chọn. Nếu bỏ trống (null), server tự đọc các món đang tick chọn trong gio_hang của khách.
 * - danhMucId: chỉ dùng khi client gửi kèm tamTinh thủ công (không đọc giỏ) để test/xem thử mã có ràng buộc
 *   danh mục (VD: CONGNGHE25 chỉ áp dụng cho dm_cong_nghe và danh mục con của nó).
 */
public record ApplyVoucherRequest(
        @NotBlank(message = "Mã giảm giá không được để trống.")
        String maCode,

        @DecimalMin(value = "0", message = "Số tiền tạm tính không được âm.")
        BigDecimal tamTinh,

        String danhMucId
) {
}
