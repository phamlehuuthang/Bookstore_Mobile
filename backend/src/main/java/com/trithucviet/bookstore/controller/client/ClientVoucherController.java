package com.trithucviet.bookstore.controller.client;

import com.trithucviet.bookstore.dto.request.ApplyVoucherRequest;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.VoucherDto;
import com.trithucviet.bookstore.dto.response.VoucherValidationResponse;
import com.trithucviet.bookstore.service.VoucherService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Mã giảm giá Voucher cho Mobile App (Bước 4 - phần 2 / Mục 5.2).
 *
 * Lưu ý phân quyền:
 * - Không gắn @PreAuthorize ở cấp class vì GET /api/v1/vouchers/active được SecurityConfig mở công khai
 *   (permitAll) để khách chưa đăng nhập vẫn xem được voucher ở trang Chi tiết sách.
 * - POST /api/v1/vouchers/validate gắn @PreAuthorize("hasRole('CUSTOMER')") để chỉ khách hàng đã đăng nhập
 *   mới kiểm tra/áp dụng voucher (và dùng authentication.getName() đọc giỏ hàng thật nếu không gửi tamTinh).
 */
@RestController
@RequestMapping("/api/v1/vouchers")
public class ClientVoucherController {

    private final VoucherService voucherService;

    public ClientVoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    // Lấy danh sách các mã giảm giá đang hoạt động, còn hạn và còn lượt dùng (không cần đăng nhập)
    @GetMapping("/active")
    public ApiResponse<List<VoucherDto>> getActiveVouchers() {
        return ApiResponse.ok("Lấy danh sách mã giảm giá thành công", voucherService.getActiveVouchers());
    }

    // Kiểm tra điều kiện áp mã và trả về số tiền được giảm dự kiến (bắt buộc CUSTOMER JWT)
    @PostMapping("/validate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<VoucherValidationResponse> validateVoucher(
            Authentication authentication,
            @Valid @RequestBody ApplyVoucherRequest request
    ) {
        return ApiResponse.ok(
                "Áp dụng mã giảm giá thành công",
                voucherService.validateVoucher(authentication.getName(), request)
        );
    }
}
