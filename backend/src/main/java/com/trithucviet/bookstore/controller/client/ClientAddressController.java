package com.trithucviet.bookstore.controller.client;

import com.trithucviet.bookstore.dto.request.AddressUpsertRequest;
import com.trithucviet.bookstore.dto.response.AddressDto;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.service.UserAddressService;
import jakarta.validation.Valid;
import java.util.List;
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
 * API Sổ địa chỉ nhận hàng cho Mobile App (Bước 2 - phần 2, Mục 2.1 & 2.2).
 *
 * Ghi chú phân quyền & kỹ thuật:
 * - @PreAuthorize("hasRole('CUSTOMER')"): chỉ tài khoản khách hàng được quản lý sổ địa chỉ cá nhân.
 * - Luôn lấy nguoiDungId qua authentication.getName() từ JWT để chống IDOR.
 * - Ghi tường minh @PathVariable("id") để chạy ổn định cả khi nhấn Run trong IDE.
 */
@RestController
@RequestMapping("/api/v1/addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class ClientAddressController {

    private final UserAddressService userAddressService;

    public ClientAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    // Lấy toàn bộ danh sách địa chỉ nhận hàng (địa chỉ mặc định luôn ở vị trí đầu)
    @GetMapping
    public ApiResponse<List<AddressDto>> getAddresses(Authentication authentication) {
        return ApiResponse.ok(
                "Lấy sổ địa chỉ thành công",
                userAddressService.getAddresses(authentication.getName())
        );
    }

    // Thêm mới 1 địa chỉ nhận hàng (nếu là địa chỉ đầu tiên hoặc macDinh = true sẽ tự làm mặc định)
    @PostMapping
    public ApiResponse<AddressDto> createAddress(
            Authentication authentication,
            @Valid @RequestBody AddressUpsertRequest request
    ) {
        return ApiResponse.ok(
                "Thêm địa chỉ nhận hàng thành công",
                userAddressService.createAddress(authentication.getName(), request)
        );
    }

    // Cập nhật thông tin 1 địa chỉ nhận hàng thuộc sở hữu của khách đang đăng nhập
    @PutMapping("/{id}")
    public ApiResponse<AddressDto> updateAddress(
            Authentication authentication,
            @PathVariable("id") String id,
            @Valid @RequestBody AddressUpsertRequest request
    ) {
        return ApiResponse.ok(
                "Cập nhật địa chỉ nhận hàng thành công",
                userAddressService.updateAddress(authentication.getName(), id, request)
        );
    }

    // Đặt nhanh 1 địa chỉ làm mặc định và tự động tắt mặc định ở các địa chỉ khác
    @PatchMapping("/{id}/default")
    public ApiResponse<AddressDto> setDefaultAddress(
            Authentication authentication,
            @PathVariable("id") String id
    ) {
        return ApiResponse.ok(
                "Đã đặt làm địa chỉ mặc định",
                userAddressService.setDefaultAddress(authentication.getName(), id)
        );
    }

    // Xóa 1 địa chỉ; nếu xóa địa chỉ mặc định mà còn địa chỉ khác thì tự đôn 1 địa chỉ lên mặc định
    @DeleteMapping("/{id}")
    public ApiResponse<List<AddressDto>> deleteAddress(
            Authentication authentication,
            @PathVariable("id") String id
    ) {
        return ApiResponse.ok(
                "Đã xóa địa chỉ nhận hàng",
                userAddressService.deleteAddress(authentication.getName(), id)
        );
    }
}
