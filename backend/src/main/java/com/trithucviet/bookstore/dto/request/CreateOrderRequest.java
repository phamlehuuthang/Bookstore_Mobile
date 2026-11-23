package com.trithucviet.bookstore.dto.request;

import com.trithucviet.bookstore.entity.enums.PhuongThucThanhToan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi khách hàng bấm Đặt hàng (POST /api/v1/orders).
 * - soDiaChiId: tùy chọn; nếu để trống Backend tự lấy địa chỉ mặc định (macDinh = true) trong sổ địa chỉ.
 * - maGiamGiaCode: tùy chọn; nếu truyền lên sẽ được kiểm tra lại điều kiện qua VoucherService trước khi trừ tiền.
 */
public record CreateOrderRequest(
        String soDiaChiId,

        @NotNull(message = "Vui lòng chọn phương thức thanh toán.")
        PhuongThucThanhToan phuongThucThanhToan,

        @Size(max = 30, message = "Mã giảm giá tối đa 30 ký tự.")
        String maGiamGiaCode,

        @Size(max = 500, message = "Ghi chú giao hàng tối đa 500 ký tự.")
        String ghiChu
) {
}
