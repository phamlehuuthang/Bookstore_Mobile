package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Dữ liệu đầu vào khi khách hàng gửi đánh giá 1-5 sao cho sách trong đơn hàng đã giao (POST /api/v1/reviews).
 * - sachId: tùy chọn; nếu bỏ trống Backend tự chọn cuốn sách đầu tiên trong đơn hàng.
 */
public record CreateReviewRequest(
        @NotBlank(message = "Mã đơn hàng không được để trống.")
        String donHangId,

        String sachId,

        @NotNull(message = "Vui lòng chọn số sao đánh giá (1 đến 5).")
        @Min(value = 1, message = "Số sao tối thiểu là 1.")
        @Max(value = 5, message = "Số sao tối đa là 5.")
        Integer soSao,

        @NotBlank(message = "Nội dung nhận xét không được để trống.")
        @Size(max = 1000, message = "Nội dung nhận xét tối đa 1000 ký tự.")
        String binhLuan,

        List<String> anhDanhGiaUrls
) {
}
