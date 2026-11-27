package com.trithucviet.bookstore.controller.admin;

import com.trithucviet.bookstore.dto.response.AdminDashboardDto;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.service.DashboardReportService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Thống kê Dashboard theo phân quyền & Xuất Báo cáo Doanh thu CSV (Bước 8 - Khối B & C).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminDashboardReportController {

    private final DashboardReportService dashboardReportService;

    public AdminDashboardReportController(DashboardReportService dashboardReportService) {
        this.dashboardReportService = dashboardReportService;
    }

    // Dashboard KPI: ADMIN nhận đủ tài chính + kho + đơn; WAREHOUSE_MANAGER & CSKH nhận dữ liệu đã ẩn doanh thu
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER', 'CSKH')")
    public ApiResponse<AdminDashboardDto> getDashboard(Authentication authentication) {
        return ApiResponse.ok(
                "Lấy dữ liệu thống kê Dashboard thành công",
                dashboardReportService.getDashboard(authentication.getName())
        );
    }

    // Xuất báo cáo doanh thu Excel CSV: CHỈ ADMIN được phép truy cập
    @GetMapping("/reports/revenue-export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportRevenueCsv(Authentication authentication) {
        return buildCsvAttachment(
                dashboardReportService.exportRevenueCsv(authentication.getName()),
                "Bao_Cao_Doanh_Thu_BookStore_" + LocalDate.now() + ".csv"
        );
    }

    // Xuất báo cáo danh sách đơn hàng Excel CSV (Phần B3): CHỈ ADMIN
    @GetMapping("/reports/orders-export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportOrdersCsv(Authentication authentication) {
        return buildCsvAttachment(
                dashboardReportService.exportOrdersCsv(authentication.getName()),
                "Bao_Cao_Don_Hang_BookStore_" + LocalDate.now() + ".csv"
        );
    }

    // Xuất báo cáo tồn kho & bán chạy 186 đầu sách Excel CSV (Phần B3): CHỈ ADMIN
    @GetMapping("/reports/inventory-export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportInventoryCsv(Authentication authentication) {
        return buildCsvAttachment(
                dashboardReportService.exportInventoryCsv(authentication.getName()),
                "Bao_Cao_Ton_Kho_BookStore_" + LocalDate.now() + ".csv"
        );
    }

    private ResponseEntity<byte[]> buildCsvAttachment(String csv, String filename) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
