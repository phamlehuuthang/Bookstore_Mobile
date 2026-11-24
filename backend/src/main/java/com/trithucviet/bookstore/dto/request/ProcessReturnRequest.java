package com.trithucviet.bookstore.dto.request;

import com.trithucviet.bookstore.entity.enums.TrangThaiDoiTra;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi ADMIN / CSKH phê duyệt (DA_CHAP_THUAN) hoặc từ chối (TU_CHOI) yêu cầu đổi trả.
 */
public record ProcessReturnRequest(
        @NotNull(message = "Vui lòng chọn quyết định xử lý (DA_CHAP_THUAN hoặc TU_CHOI).")
        TrangThaiDoiTra trangThai,

        @Size(max = 500, message = "Ghi chú phản hồi tối đa 500 ký tự.")
        String ghiChuAdmin
) {
}
