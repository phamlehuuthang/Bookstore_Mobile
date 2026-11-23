package com.trithucviet.bookstore.dto.response;

/**
 * Thông tin Tác giả kèm số lượng sách liên kết phục vụ Web Admin (Bước 3 - phần 4).
 */
public record AuthorDto(
        String id,
        String tenTacGia,
        String tieuSu,
        long soLuongSach
) {
}
