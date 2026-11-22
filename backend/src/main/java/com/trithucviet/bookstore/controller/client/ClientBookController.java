package com.trithucviet.bookstore.controller.client;

import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.BookDetailDto;
import com.trithucviet.bookstore.dto.response.BookPageResponse;
import com.trithucviet.bookstore.dto.response.CategoryDto;
import com.trithucviet.bookstore.dto.response.HomeBooksResponse;
import com.trithucviet.bookstore.dto.response.ReviewDto;
import com.trithucviet.bookstore.service.BookService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API công khai Danh mục, Trang chủ, Tìm kiếm/Lọc/Sắp xếp & Chi tiết Sách cho Mobile App (Bước 3 - phần 1 & 2).
 *
 * Ghi chú kỹ thuật:
 * - Toàn bộ các endpoint GET /api/v1/categories/** và GET /api/v1/books/** đã được SecurityConfig mở permitAll(),
 *   cho phép khách vãng lai xem và tìm kiếm sách không cần đăng nhập.
 * - Mọi @PathVariable và @RequestParam đều ghi tường minh tên tham số để chạy ổn định khi nhấn Run trực tiếp từ IDE.
 */
@RestController
public class ClientBookController {

    private final BookService bookService;

    public ClientBookController(BookService bookService) {
        this.bookService = bookService;
    }

    // Lấy cây danh mục Cha - Con đang hoạt động kèm số lượng sách đang bán
    @GetMapping("/api/v1/categories")
    public ApiResponse<List<CategoryDto>> getActiveCategories() {
        return ApiResponse.ok("Lấy danh mục sách thành công", bookService.getActiveCategoryTree());
    }

    // Lấy kệ Flash Sale & Sách bán chạy cho Trang chủ (mặc định 12 cuốn mỗi kệ)
    @GetMapping("/api/v1/books/home")
    public ApiResponse<HomeBooksResponse> getHomeBooks(
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        return ApiResponse.ok("Lấy sách trang chủ thành công", bookService.getHomeBooks(limit));
    }

    // Tìm kiếm, lọc đa tiêu chí, sắp xếp (8 chế độ chuẩn TMĐT) và phân trang danh sách sách
    @GetMapping("/api/v1/books")
    public ApiResponse<BookPageResponse> searchBooks(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "categoryId", required = false) String categoryId,
            @RequestParam(name = "authorId", required = false) String authorId,
            @RequestParam(name = "publisherId", required = false) String publisherId,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "minRating", required = false) BigDecimal minRating,
            @RequestParam(name = "flashSaleOnly", required = false) Boolean flashSaleOnly,
            @RequestParam(name = "bestSellerOnly", required = false) Boolean bestSellerOnly,
            @RequestParam(name = "inStockOnly", required = false) Boolean inStockOnly,
            @RequestParam(name = "coverType", required = false) String coverType,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        return ApiResponse.ok(
                "Lấy danh sách sách thành công",
                bookService.searchAndFilterBooks(
                        search,
                        categoryId,
                        authorId,
                        publisherId,
                        minPrice,
                        maxPrice,
                        minRating,
                        flashSaleOnly,
                        bestSellerOnly,
                        inStockOnly,
                        coverType,
                        sortBy,
                        page,
                        size
                )
        );
    }

    // Xem chi tiết 1 cuốn sách kèm tối đa 6 cuốn sách cùng chủ đề
    @GetMapping("/api/v1/books/{id}")
    public ApiResponse<BookDetailDto> getBookDetail(@PathVariable("id") String id) {
        return ApiResponse.ok("Lấy chi tiết sách thành công", bookService.getBookDetail(id));
    }

    // Lấy danh sách đánh giá công khai (biAn = false) của 1 cuốn sách
    @GetMapping("/api/v1/books/{id}/reviews")
    public ApiResponse<List<ReviewDto>> getBookReviews(@PathVariable("id") String id) {
        return ApiResponse.ok("Lấy đánh giá sách thành công", bookService.getBookReviews(id));
    }
}
