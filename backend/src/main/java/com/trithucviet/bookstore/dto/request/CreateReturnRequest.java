package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Dữ liệu đầu vào khi khách hàng gửi yêu cầu Đổi/Trả hàng cho đơn đã giao (POST /api/v1/returns).
 */
public record CreateReturnRequest(
        @NotBlank(message = "Mã đơn hàng không được để trống.")
        String donHangId,

        @NotBlank(message = "Vui lòng chọn hoặc nhập lý do đổi trả.")
        @Size(max = 255, message = "Lý do đổi trả tối đa 255 ký tự.")
        String lyDo,

        @NotBlank(message = "Vui lòng mô tả chi tiết tình trạng sách.")
        @Size(max = 1000, message = "Mô tả chi tiết tối đa 1000 ký tự.")
        String moTa,

        List<String> anhMinhChungUrls
) {
}
