package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu đầu vào khi Admin / Quản lý Kho thêm hoặc cập nhật Tác giả (Bước 3 - phần 4).
 */
public record AuthorUpsertRequest(
        @NotBlank(message = "Tên tác giả không được để trống.")
        @Size(max = 150, message = "Tên tác giả tối đa 150 ký tự.")
        String tenTacGia,

        @Size(max = 500, message = "Tiểu sử tác giả tối đa 500 ký tự.")
        String tieuSu
) {
}
