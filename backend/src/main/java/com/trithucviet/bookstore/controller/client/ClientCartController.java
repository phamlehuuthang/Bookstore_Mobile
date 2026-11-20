package com.trithucviet.bookstore.controller.client;

import com.trithucviet.bookstore.dto.request.CartItemRequest;
import com.trithucviet.bookstore.dto.request.CartSelectAllRequest;
import com.trithucviet.bookstore.dto.request.CartUpdateRequest;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.CartResponseDto;
import com.trithucviet.bookstore.service.CartWishlistService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Giỏ hàng cho Mobile App (Bước 4 - phần 2, Mục 5.1).
 *
 * Ghi chú phân quyền & kỹ thuật:
 * - @PreAuthorize("hasRole('CUSTOMER')"): chặn các tài khoản nhân sự (ADMIN, WAREHOUSE_MANAGER, CSKH)
 *   không được thao tác giỏ hàng cá nhân của khách hàng.
 * - Luôn lấy nguoiDungId qua authentication.getName() do JwtAuthenticationFilter nạp từ JWT để chống IDOR.
 * - Ghi tường minh @PathVariable("bookId") để chạy ổn định khi nhấn Run trực tiếp từ IDE.
 */
@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class ClientCartController {

    private final CartWishlistService cartWishlistService;

    public ClientCartController(CartWishlistService cartWishlistService) {
        this.cartWishlistService = cartWishlistService;
    }

    // Lấy toàn bộ giỏ hàng kèm bảng tạm tính (chỉ tính các món đã tick chọn)
    @GetMapping
    public ApiResponse<CartResponseDto> getCart(Authentication authentication) {
        return ApiResponse.ok("Lấy giỏ hàng thành công", cartWishlistService.getCart(authentication.getName()));
    }

    // Thêm sách vào giỏ (nếu sách đã có trong giỏ sẽ cộng dồn số lượng và kiểm tra không vượt tồn kho)
    @PostMapping
    public ApiResponse<CartResponseDto> addToCart(
            Authentication authentication,
            @Valid @RequestBody CartItemRequest request
    ) {
        return ApiResponse.ok("Đã thêm sách vào giỏ hàng", cartWishlistService.addToCart(authentication.getName(), request));
    }

    // Cập nhật số lượng (>= 1) hoặc trạng thái tick chọn của 1 cuốn sách trong giỏ
    @PutMapping("/{bookId}")
    public ApiResponse<CartResponseDto> updateCartItem(
            Authentication authentication,
            @PathVariable("bookId") String bookId,
            @Valid @RequestBody CartUpdateRequest request
    ) {
        return ApiResponse.ok(
                "Cập nhật giỏ hàng thành công",
                cartWishlistService.updateCartItem(authentication.getName(), bookId, request)
        );
    }

    // Endpoint bổ sung phục vụ nút "Chọn tất cả / Bỏ chọn tất cả" trên cart.tsx chỉ qua 1 request duy nhất
    @PatchMapping("/select-all")
    public ApiResponse<CartResponseDto> selectAll(
            Authentication authentication,
            @Valid @RequestBody CartSelectAllRequest request
    ) {
        String message = Boolean.TRUE.equals(request.daChon())
                ? "Đã chọn tất cả sản phẩm trong giỏ"
                : "Đã bỏ chọn tất cả sản phẩm trong giỏ";
        return ApiResponse.ok(
                message,
                cartWishlistService.selectAllCartItems(authentication.getName(), request.daChon())
        );
    }

    // Xóa 1 cuốn sách khỏi giỏ hàng (dùng sau khi khách bấm xác nhận trên hộp thoại xóa)
    @DeleteMapping("/{bookId}")
    public ApiResponse<CartResponseDto> removeCartItem(
            Authentication authentication,
            @PathVariable("bookId") String bookId
    ) {
        return ApiResponse.ok(
                "Đã xóa sách khỏi giỏ hàng",
                cartWishlistService.removeCartItem(authentication.getName(), bookId)
        );
    }
}
