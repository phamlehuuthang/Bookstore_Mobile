package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi khách hàng hủy đơn đang ở trạng thái CHO_XAC_NHAN (POST /api/v1/orders/{id}/cancel).
 */
public record CancelOrderRequest(
        @NotBlank(message = "Vui lòng nhập lý do hủy đơn hàng.")
        @Size(max = 400, message = "Lý do hủy đơn tối đa 400 ký tự.")
        String lyDo
) {
}
