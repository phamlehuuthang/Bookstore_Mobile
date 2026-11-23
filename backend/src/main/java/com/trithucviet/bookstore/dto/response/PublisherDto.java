package com.trithucviet.bookstore.dto.response;

/**
 * Thông tin Nhà xuất bản kèm số lượng sách liên kết phục vụ Web Admin (Bước 3 - phần 4).
 */
public record PublisherDto(
        String id,
        String tenNxb,
        String diaChi,
        long soLuongSach
) {
}
