package com.trithucviet.bookstore.controller.admin;

import com.trithucviet.bookstore.dto.request.AuthorUpsertRequest;
import com.trithucviet.bookstore.dto.request.BookUpsertRequest;
import com.trithucviet.bookstore.dto.request.CategoryUpsertRequest;
import com.trithucviet.bookstore.dto.request.PublisherUpsertRequest;
import com.trithucviet.bookstore.dto.response.AdminBookDto;
import com.trithucviet.bookstore.dto.response.AdminCategoryDto;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.AuthorDto;
import com.trithucviet.bookstore.dto.response.PublisherDto;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.service.BookService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Quản trị Sách, Danh mục Cha–Con, Tác giả & Nhà xuất bản trên Web Admin (Bước 3 - Phần 3 & 4).
 *
 * Ma trận phân quyền theo PHAN_QUYEN_WEB_ADMIN.md:
 * - ADMIN: toàn quyền xem, thêm, sửa (kể cả giá bán & Flash Sale), bật/tắt hiển thị, xóa.
 * - WAREHOUSE_MANAGER: xem, thêm, sửa thông tin sách/danh mục/tác giả/NXB;
 *   bị chặn sửa giá bán & Flash Sale (403 ở tầng Service) và bị chặn xóa/ẩn sách (403 ở tầng Controller).
 * - CSKH: chỉ được xem (GET) để tra cứu tư vấn khách; mọi thao tác POST/PUT/PATCH/DELETE bị chặn 403.
 * - Ghi tường minh tên tham số trong @PathVariable và @RequestParam để chạy ổn định khi nhấn Run từ IDE.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminBookCategoryController {

    private final BookService bookService;

    public AdminBookCategoryController(BookService bookService) {
        this.bookService = bookService;
    }

    // --- 1. QUẢN LÝ SÁCH (BƯỚC 3 - PHẦN 3) ---

    @GetMapping("/books")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'CSKH')")
    public ApiResponse<List<AdminBookDto>> getAdminBooks(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "danhMucId", required = false) String danhMucId,
            @RequestParam(name = "hienThi", required = false) Boolean hienThi
    ) {
        return ApiResponse.ok(
                "Lấy danh sách sách quản trị thành công",
                bookService.getAdminBooks(keyword, danhMucId, hienThi)
        );
    }

    @GetMapping("/books/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'CSKH')")
    public ApiResponse<AdminBookDto> getAdminBookById(@PathVariable("id") String id) {
        return ApiResponse.ok(bookService.getAdminBookById(id));
    }

    @PostMapping("/books")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    public ApiResponse<AdminBookDto> createBook(
            Authentication authentication,
            @Valid @RequestBody BookUpsertRequest request
    ) {
        return ApiResponse.ok(
                "Thêm đầu sách mới thành công",
                bookService.createBook(request, resolveRole(authentication))
        );
    }

    @PutMapping("/books/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    public ApiResponse<AdminBookDto> updateBook(
            Authentication authentication,
            @PathVariable("id") String id,
            @Valid @RequestBody BookUpsertRequest request
    ) {
        return ApiResponse.ok(
                "Cập nhật thông tin sách thành công",
                bookService.updateBook(id, request, resolveRole(authentication))
        );
    }

    @PatchMapping("/books/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminBookDto> toggleBookVisibility(@PathVariable("id") String id) {
        AdminBookDto updated = bookService.toggleBookVisibility(id);
        String message = Boolean.TRUE.equals(updated.hienThi())
                ? "Đã bật hiển thị sách trên App"
                : "Đã ẩn sách khỏi App";
        return ApiResponse.ok(message, updated);
    }

    @DeleteMapping("/books/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteBook(@PathVariable("id") String id) {
        bookService.deleteBook(id);
        return ApiResponse.ok("Đã xóa đầu sách khỏi hệ thống", null);
    }

    // --- 2. QUẢN LÝ DANH MỤC CHA - CON (BƯỚC 3 - PHẦN 4) ---

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'CSKH')")
    public ApiResponse<List<AdminCategoryDto>> getAdminCategories() {
        return ApiResponse.ok("Lấy danh sách danh mục thành công", bookService.getAdminCategories());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    public ApiResponse<AdminCategoryDto> createCategory(@Valid @RequestBody CategoryUpsertRequest request) {
        return ApiResponse.ok("Thêm danh mục mới thành công", bookService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    public ApiResponse<AdminCategoryDto> updateCategory(
            @PathVariable("id") String id,
            @Valid @RequestBody CategoryUpsertRequest request
    ) {
        return ApiResponse.ok("Cập nhật danh mục thành công", bookService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteCategory(@PathVariable("id") String id) {
        bookService.deleteCategory(id);
        return ApiResponse.ok("Đã xóa danh mục", null);
    }

    // --- 3. QUẢN LÝ TÁC GIẢ (BƯỚC 3 - PHẦN 4) ---

    @GetMapping("/authors")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'CSKH')")
    public ApiResponse<List<AuthorDto>> getAdminAuthors() {
        return ApiResponse.ok("Lấy danh sách tác giả thành công", bookService.getAdminAuthors());
    }

    @PostMapping("/authors")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    public ApiResponse<AuthorDto> createAuthor(@Valid @RequestBody AuthorUpsertRequest request) {
        return ApiResponse.ok("Thêm tác giả mới thành công", bookService.createAuthor(request));
    }

    @PutMapping("/authors/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    public ApiResponse<AuthorDto> updateAuthor(
            @PathVariable("id") String id,
            @Valid @RequestBody AuthorUpsertRequest request
    ) {
        return ApiResponse.ok("Cập nhật tác giả thành công", bookService.updateAuthor(id, request));
    }

    @DeleteMapping("/authors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteAuthor(@PathVariable("id") String id) {
        bookService.deleteAuthor(id);
        return ApiResponse.ok("Đã xóa tác giả", null);
    }

    // --- 4. QUẢN LÝ NHÀ XUẤT BẢN (BƯỚC 3 - PHẦN 4) ---

    @GetMapping("/publishers")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'CSKH')")
    public ApiResponse<List<PublisherDto>> getAdminPublishers() {
        return ApiResponse.ok("Lấy danh sách nhà xuất bản thành công", bookService.getAdminPublishers());
    }

    @PostMapping("/publishers")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    public ApiResponse<PublisherDto> createPublisher(@Valid @RequestBody PublisherUpsertRequest request) {
        return ApiResponse.ok("Thêm nhà xuất bản mới thành công", bookService.createPublisher(request));
    }

    @PutMapping("/publishers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    public ApiResponse<PublisherDto> updatePublisher(
            @PathVariable("id") String id,
            @Valid @RequestBody PublisherUpsertRequest request
    ) {
        return ApiResponse.ok("Cập nhật nhà xuất bản thành công", bookService.updatePublisher(id, request));
    }

    @DeleteMapping("/publishers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deletePublisher(@PathVariable("id") String id) {
        bookService.deletePublisher(id);
        return ApiResponse.ok("Đã xóa nhà xuất bản", null);
    }

    private VaiTro resolveRole(Authentication authentication) {
        if (authentication == null) {
            return VaiTro.CUSTOMER;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String raw = authority.getAuthority();
            if (raw != null && raw.startsWith("ROLE_")) {
                try {
                    return VaiTro.valueOf(raw.substring("ROLE_".length()));
                } catch (IllegalArgumentException ignored) {
                    // Tiếp tục kiểm tra authority tiếp theo nếu không khớp enum VaiTro
                }
            }
        }
        return VaiTro.CUSTOMER;
    }
}
