package com.trithucviet.bookstore.controller.admin;

import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.StoreConfigDto;
import com.trithucviet.bookstore.service.StoreConfigService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Cấu hình Cửa hàng (Phần B1):
 * - GET /api/v1/store-config: công khai cho Mobile App đọc thông tin thương hiệu, phí ship, ngưỡng Freeship & tài khoản VietQR.
 * - GET /api/admin/settings: nhân sự Web Admin xem cấu hình.
 * - PUT /api/admin/settings: chỉ ADMIN được phép cập nhật cấu hình xuống PostgreSQL.
 */
@RestController
public class StoreConfigController {

    private final StoreConfigService storeConfigService;

    public StoreConfigController(StoreConfigService storeConfigService) {
        this.storeConfigService = storeConfigService;
    }

    @GetMapping("/api/v1/store-config")
    public ApiResponse<StoreConfigDto> getPublicStoreConfig() {
        return ApiResponse.ok("Lấy cấu hình cửa hàng thành công", storeConfigService.getStoreConfig());
    }

    @GetMapping("/api/admin/settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'CSKH')")
    public ApiResponse<StoreConfigDto> getAdminStoreConfig() {
        return ApiResponse.ok("Lấy cấu hình cửa hàng thành công", storeConfigService.getStoreConfig());
    }

    @PutMapping("/api/admin/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StoreConfigDto> updateAdminStoreConfig(@Valid @RequestBody StoreConfigDto request) {
        return ApiResponse.ok("Đã lưu cấu hình cửa hàng xuống cơ sở dữ liệu", storeConfigService.updateStoreConfig(request));
    }
}
