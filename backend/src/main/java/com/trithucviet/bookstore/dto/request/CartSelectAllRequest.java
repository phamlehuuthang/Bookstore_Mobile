package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Dữ liệu đầu vào cho API "Chọn tất cả / Bỏ chọn tất cả" trong giỏ hàng (PATCH /api/v1/cart/select-all).
 */
public record CartSelectAllRequest(
        @NotNull(message = "Trạng thái chọn tất cả không được để trống.")
        Boolean daChon
) {
}
