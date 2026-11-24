package com.trithucviet.bookstore.controller.admin;

import com.trithucviet.bookstore.dto.request.CreateInventoryReceiptRequest;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.DashboardBookItemDto;
import com.trithucviet.bookstore.dto.response.InventoryReceiptDto;
import com.trithucviet.bookstore.service.InventoryService;
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
 * API Quản lý Phiếu Nhập Kho & Cảnh báo Sách sắp hết hàng trên Web Admin (Bước 8 - Khối A).
 *
 * Phân quyền theo PHAN_QUYEN_WEB_ADMIN.md:
 * - ADMIN & WAREHOUSE_MANAGER: toàn quyền xem lịch sử phiếu nhập, xem cảnh báo hết hàng và lập phiếu nhập mới.
 * - CSKH & CUSTOMER: bị chặn hoàn toàn (403 Forbidden).
 */
@RestController
@RequestMapping("/api/admin/inventory")
@PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    public AdminInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // Xem danh sách lịch sử phiếu nhập kho kèm chi tiết đầu sách
    @GetMapping("/receipts")
    public ApiResponse<List<InventoryReceiptDto>> getReceipts() {
        return ApiResponse.ok(
                "Lấy danh sách phiếu nhập kho thành công",
                inventoryService.getAllReceipts()
        );
    }

    // Lập phiếu nhập kho mới -> tự động cộng tồn kho cho từng cuốn sách trong phiếu
    @PostMapping("/receipts")
    public ApiResponse<InventoryReceiptDto> createReceipt(
            Authentication authentication,
            @Valid @RequestBody CreateInventoryReceiptRequest request
    ) {
        return ApiResponse.ok(
                "Tạo phiếu nhập kho và cập nhật tồn kho thành công",
                inventoryService.createReceipt(authentication.getName(), request)
        );
    }

    // Danh sách đầu sách có tồn kho <= 5 cuốn cần nhập bổ sung
    @GetMapping("/low-stock")
    public ApiResponse<List<DashboardBookItemDto>> getLowStockBooks() {
        return ApiResponse.ok(
                "Lấy danh sách sách sắp hết hàng thành công",
                inventoryService.getLowStockBooks()
        );
    }
}
