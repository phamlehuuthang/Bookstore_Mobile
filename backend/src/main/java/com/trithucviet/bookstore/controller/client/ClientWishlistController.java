package com.trithucviet.bookstore.controller.client;

import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.WishlistItemResponse;
import com.trithucviet.bookstore.dto.response.WishlistToggleResponse;
import com.trithucviet.bookstore.service.CartWishlistService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Tủ sách yêu thích cho Mobile App (Bước 4 - phần 1).
 *
 * Ghi chú phân quyền & bảo mật:
 * - @PreAuthorize("hasRole('CUSTOMER')"): chặn các vai trò nhân sự nội bộ (ADMIN, WAREHOUSE_MANAGER, CSKH)
 *   không được gọi vào API cá nhân của khách hàng (trả 403 Forbidden).
 * - Không nhận userId từ request/URL mà lấy qua authentication.getName() (do JwtAuthenticationFilter nạp từ JWT)
 *   để khách hàng không thể sửa tham số để xem/xóa tủ sách của người khác (chống IDOR).
 * - Ghi tường minh @PathVariable("bookId") thay vì @PathVariable để chạy ổn định khi nhấn Run trực tiếp
 *   trên IDE (trình biên dịch IDE thường không bật cờ -parameters như Gradle bootRun).
 */
@RestController
@RequestMapping("/api/v1/wishlists")
@PreAuthorize("hasRole('CUSTOMER')")
public class ClientWishlistController {

    private final CartWishlistService cartWishlistService;

    public ClientWishlistController(CartWishlistService cartWishlistService) {
        this.cartWishlistService = cartWishlistService;
    }

    // Endpoint chính theo Mục 4.1: Lấy danh sách sách khách đang yêu thích (màn Profile > Yêu thích)
    @GetMapping
    public ApiResponse<List<WishlistItemResponse>> getWishlist(Authentication authentication) {
        return ApiResponse.ok("Lấy tủ sách yêu thích thành công", cartWishlistService.getWishlist(authentication.getName()));
    }

    // Endpoint bổ sung: Kiểm tra nhanh trạng thái tim của 1 cuốn sách (dùng ở màn Chi tiết sách nếu không tải cả tủ)
    @GetMapping("/{bookId}/status")
    public ApiResponse<WishlistToggleResponse> getStatus(Authentication authentication, @PathVariable("bookId") String bookId) {
        return ApiResponse.ok(cartWishlistService.getStatus(authentication.getName(), bookId));
    }

    // Endpoint chính theo Mục 4.1: Nút trái tim (thả tim nếu chưa thích, bỏ tim nếu đã thích)
    @PostMapping("/{bookId}/toggle")
    public ApiResponse<WishlistToggleResponse> toggle(Authentication authentication, @PathVariable("bookId") String bookId) {
        WishlistToggleResponse result = cartWishlistService.toggle(authentication.getName(), bookId);
        String message = result.daYeuThich() ? "Đã thêm vào Tủ sách yêu thích" : "Đã bỏ khỏi Tủ sách yêu thích";
        return ApiResponse.ok(message, result);
    }

    // Endpoint bổ sung chuẩn RESTful: Xóa tường minh khỏi tủ (tránh trường hợp bấm đúp toggle thành thêm lại)
    @DeleteMapping("/{bookId}")
    public ApiResponse<WishlistToggleResponse> remove(Authentication authentication, @PathVariable("bookId") String bookId) {
        return ApiResponse.ok("Đã bỏ khỏi Tủ sách yêu thích", cartWishlistService.remove(authentication.getName(), bookId));
    }
}
