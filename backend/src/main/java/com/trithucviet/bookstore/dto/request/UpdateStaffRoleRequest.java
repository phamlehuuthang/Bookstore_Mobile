package com.trithucviet.bookstore.dto.request;

import com.trithucviet.bookstore.entity.enums.VaiTro;
import jakarta.validation.constraints.NotNull;

/**
 * Dữ liệu đầu vào khi Admin thay đổi vai trò (RBAC) của 1 nhân sự nội bộ.
 */
public record UpdateStaffRoleRequest(
        @NotNull(message = "Vai trò mới không được để trống.")
        VaiTro vaiTro
) {
}
