package com.trithucviet.bookstore.dto.response;

import java.util.List;

/**
 * Dữ liệu các kệ sách nổi bật cho Trang chủ Mobile App (GET /api/v1/books/home).
 */
public record HomeBooksResponse(
        List<BookDto> flashSaleBooks,
        List<BookDto> bestSellerBooks,
        long tongSoSachFlashSale,
        long tongSoSachBanChay,
        long tongSoSachHienThi
) {
}
