package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Dữ liệu đầu vào khi khách hàng bấm "Thêm vào giỏ hàng" (POST /api/v1/cart).
 */
public record CartItemRequest(
        @NotBlank(message = "Mã sách không được để trống.")
        String sachId,

        @NotNull(message = "Số lượng không được để trống.")
        @Min(value = 1, message = "Số lượng thêm vào giỏ phải từ 1 cuốn trở lên.")
        Integer soLuong
) {
}
