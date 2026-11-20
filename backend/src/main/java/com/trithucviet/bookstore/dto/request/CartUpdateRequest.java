package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.Min;

/**
 * Dữ liệu đầu vào khi khách hàng chỉnh số lượng hoặc tick chọn từng món trong giỏ (PUT /api/v1/cart/{bookId}).
 *
 * Lưu ý thiết kế:
 * - Cho phép truyền riêng lẻ soLuong, riêng lẻ daChon, hoặc cả 2 trường trong cùng 1 request.
 * - Cố tình chặn soLuong <= 0 bằng @Min(1) (Phương án B - tách bạch với DELETE /api/v1/cart/{bookId}):
 *   khi số lượng là 1 thì trên Mobile nút dấu trừ bị làm mờ, muốn xóa hẳn phải bấm nút xóa có xác nhận.
 */
public record CartUpdateRequest(
        @Min(value = 1, message = "Số lượng tối thiểu là 1 cuốn.")
        Integer soLuong,

        Boolean daChon
) {
}
