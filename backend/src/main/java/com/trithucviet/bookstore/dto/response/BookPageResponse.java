package com.trithucviet.bookstore.dto.response;

import java.util.List;

/**
 * Kết quả phân trang danh sách sách sau khi áp dụng bộ lọc & sắp xếp (GET /api/v1/books).
 */
public record BookPageResponse(
        List<BookDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        String appliedSort
) {
}
