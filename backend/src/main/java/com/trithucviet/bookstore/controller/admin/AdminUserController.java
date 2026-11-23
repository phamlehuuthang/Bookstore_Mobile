package com.trithucviet.bookstore.controller.admin;

import com.trithucviet.bookstore.dto.request.CreateStaffRequest;
import com.trithucviet.bookstore.dto.request.UpdateStaffRoleRequest;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.UserDto;
import com.trithucviet.bookstore.service.UserAddressService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Quản lý Khách hàng & Phân quyền Nhân sự trên Web Admin (Bước 2 - Phần 3).
 *
 * Ma trận phân quyền theo PHAN_QUYEN_WEB_ADMIN.md:
 * - ADMIN: toàn quyền xem khách hàng, xem/tạo nhân sự, khóa/mở tài khoản, đổi vai trò nhân sự.
 * - CSKH: chỉ được xem danh sách khách hàng (GET /customers) để hỗ trợ đơn; bị chặn 403 với các API nhân sự & khóa tài khoản.
 * - WAREHOUSE_MANAGER & CUSTOMER: bị chặn hoàn toàn (403 Forbidden).
 * - Ghi tường minh tên tham số trong @PathVariable("id") và @RequestParam(name = "...")
 *   để chạy ổn định cả khi nhấn Run trực tiếp từ IDE.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserAddressService userAddressService;

    public AdminUserController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    // Xem danh sách tài khoản Khách hàng (ADMIN và CSKH đều xem được; hỗ trợ tìm theo tên/email/SĐT)
    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'CSKH')")
    public ApiResponse<List<UserDto>> getCustomers(
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.ok(
                "Lấy danh sách khách hàng thành công",
                userAddressService.getCustomers(keyword)
        );
    }

    // Xem danh sách tài khoản Nhân sự nội bộ (chỉ ADMIN)
    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserDto>> getStaff(
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.ok(
                "Lấy danh sách nhân sự thành công",
                userAddressService.getStaff(keyword)
        );
    }

    // Tạo mới tài khoản nhân sự ADMIN / WAREHOUSE_MANAGER / CSKH (chỉ ADMIN)
    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDto> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ApiResponse.ok(
                "Cấp tài khoản nhân sự mới thành công",
                userAddressService.createStaffAccount(request)
        );
    }

    // Khóa hoặc mở khóa tài khoản người dùng; chặn tự khóa chính tài khoản đang đăng nhập (chỉ ADMIN)
    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDto> toggleUserLock(
            Authentication authentication,
            @PathVariable("id") String id
    ) {
        UserDto updated = userAddressService.toggleUserLock(authentication.getName(), id);
        String message = Boolean.TRUE.equals(updated.biKhoa())
                ? "Đã khóa tài khoản người dùng"
                : "Đã mở khóa tài khoản người dùng";
        return ApiResponse.ok(message, updated);
    }

    // Thay đổi vai trò RBAC cho tài khoản nhân sự nội bộ (chỉ ADMIN)
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserDto> updateStaffRole(
            Authentication authentication,
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateStaffRoleRequest request
    ) {
        return ApiResponse.ok(
                "Cập nhật vai trò nhân sự thành công",
                userAddressService.updateStaffRole(authentication.getName(), id, request.vaiTro())
        );
    }
}
