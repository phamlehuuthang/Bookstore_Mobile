package com.trithucviet.bookstore.dto.request;

import com.trithucviet.bookstore.entity.enums.TrangThaiDon;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi ADMIN / WAREHOUSE_MANAGER / CSKH chuyển trạng thái đơn hàng trên Web Admin
 * (PATCH /api/admin/orders/{id}/status).
 */
public record UpdateOrderStatusRequest(
        @NotNull(message = "Vui lòng chọn trạng thái đơn hàng mới.")
        TrangThaiDon trangThaiMoi,

        @Size(max = 400, message = "Ghi chú xử lý đơn tối đa 400 ký tự.")
        String ghiChu
) {
}
