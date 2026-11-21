package com.trithucviet.bookstore.controller.admin;

import com.trithucviet.bookstore.dto.request.VoucherUpsertRequest;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.VoucherDto;
import com.trithucviet.bookstore.service.VoucherService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * API Quản lý Mã giảm giá Voucher cho Web Admin (Bước 4 - phần 3).
 *
 * Phân quyền chi tiết theo PHAN_QUYEN_WEB_ADMIN.md:
 * - ADMIN: toàn quyền xem, tạo mới, sửa, bật/tắt kích hoạt voucher.
 * - CSKH: chỉ được xem (GET) để tra cứu mã tư vấn khách; bị chặn 403 khi gọi POST/PUT/PATCH.
 * - WAREHOUSE_MANAGER & CUSTOMER: bị chặn hoàn toàn (403 Forbidden).
 * - Ghi tường minh tên tham số trong @PathVariable("id") và @RequestParam(name = "...")
 *   để chạy ổn định khi nhấn Run trực tiếp từ IDE.
 */
@RestController
@RequestMapping("/api/admin/vouchers")
public class AdminVoucherController {

    private final VoucherService voucherService;

    public AdminVoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    // Lấy toàn bộ voucher cho Web Admin (có thể lọc theo trạng thái bật/tắt hoặc từ khóa mã/tiêu đề)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CSKH')")
    public ApiResponse<List<VoucherDto>> getVouchers(
            @RequestParam(name = "dangHoatDong", required = false) Boolean dangHoatDong,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.ok(
                "Lấy danh sách voucher quản trị thành công",
                voucherService.getAdminVouchers(dangHoatDong, keyword)
        );
    }

    // Xem chi tiết 1 voucher theo id
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CSKH')")
    public ApiResponse<VoucherDto> getVoucherById(@PathVariable("id") String id) {
        return ApiResponse.ok(voucherService.getVoucherById(id));
    }

    // Phát hành voucher mới (chỉ ADMIN)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VoucherDto> createVoucher(@Valid @RequestBody VoucherUpsertRequest request) {
        return ApiResponse.ok("Phát hành voucher mới thành công", voucherService.createVoucher(request));
    }

    // Cập nhật thông tin voucher (chỉ ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VoucherDto> updateVoucher(
            @PathVariable("id") String id,
            @Valid @RequestBody VoucherUpsertRequest request
    ) {
        return ApiResponse.ok("Cập nhật voucher thành công", voucherService.updateVoucher(id, request));
    }

    // Bật / tạm ngưng nhanh voucher thay cho xóa cứng để bảo toàn lịch sử don_hang (chỉ ADMIN)
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VoucherDto> toggleVoucher(@PathVariable("id") String id) {
        VoucherDto updated = voucherService.toggleVoucherActive(id);
        String message = Boolean.TRUE.equals(updated.dangHoatDong())
                ? "Đã kích hoạt mã giảm giá"
                : "Đã tạm ngưng mã giảm giá";
        return ApiResponse.ok(message, updated);
    }
}
