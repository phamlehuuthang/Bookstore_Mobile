package com.trithucviet.bookstore.controller.admin;

import com.trithucviet.bookstore.dto.request.UpdateOrderStatusRequest;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.OrderDto;
import com.trithucviet.bookstore.entity.enums.TrangThaiDon;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import com.trithucviet.bookstore.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Quản lý & Chuyển trạng thái Đơn hàng trên Web Admin (Bước 6 - Phần 3).
 *
 * Phân quyền chi tiết:
 * - ADMIN, WAREHOUSE_MANAGER, CSKH: đều xem được danh sách & chi tiết đơn hàng.
 * - Khi chuyển trạng thái (PATCH /{id}/status):
 *   + ADMIN: toàn quyền mọi chuyển trạng thái 1 chiều + hủy ở CHO_XAC_NHAN / DANG_DONG_GOI.
 *   + WAREHOUSE_MANAGER: chỉ được CHO_XAC_NHAN -> DANG_DONG_GOI và DANG_DONG_GOI -> DANG_GIAO.
 *   + CSKH: được xác nhận đơn (CHO_XAC_NHAN -> DANG_DONG_GOI), hủy đơn ở CHO_XAC_NHAN / DANG_DONG_GOI
 *           và xác nhận DA_GIAO (DANG_GIAO -> DA_GIAO).
 * - Ghi tường minh tên tham số trong @PathVariable("id") và @RequestParam(name = ...) để chạy ổn định trên IDE.
 */
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'CSKH')")
public class AdminOrderReturnController {

    private final OrderService orderService;

    public AdminOrderReturnController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Lấy toàn bộ danh sách đơn hàng cho Web Admin (hỗ trợ lọc theo trạng thái và từ khóa mã đơn/tên/SĐT)
    @GetMapping
    public ApiResponse<List<OrderDto>> getAdminOrders(
            @RequestParam(name = "trangThai", required = false) TrangThaiDon trangThai,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.ok(
                "Lấy danh sách đơn hàng quản trị thành công",
                orderService.getAdminOrders(trangThai, keyword)
        );
    }

    // Xem chi tiết 1 đơn hàng kèm danh sách sách và Timeline lịch sử xử lý
    @GetMapping("/{id}")
    public ApiResponse<OrderDto> getAdminOrderDetail(@PathVariable("id") String id) {
        return ApiResponse.ok(
                "Lấy chi tiết đơn hàng thành công",
                orderService.getAdminOrderDetail(id)
        );
    }

    // Duyệt & chuyển trạng thái đơn hàng 1 chiều theo đúng vai trò RBAC
    @PatchMapping("/{id}/status")
    public ApiResponse<OrderDto> updateOrderStatus(
            Authentication authentication,
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ApiResponse.ok(
                "Cập nhật trạng thái đơn hàng thành công",
                orderService.updateOrderStatusByStaff(id, request, resolveRole(authentication))
        );
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
                    // Tiếp tục kiểm tra authority kế tiếp nếu không khớp enum VaiTro
                }
            }
        }
        return VaiTro.CUSTOMER;
    }
}
