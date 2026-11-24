package com.trithucviet.bookstore.controller.admin;

import com.trithucviet.bookstore.dto.request.CreateNotificationRequest;
import com.trithucviet.bookstore.dto.response.AdminReviewDto;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.service.NotificationService;
import com.trithucviet.bookstore.service.ReviewReturnService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Kiểm duyệt Đánh giá & Phát sóng Thông báo đẩy trên Web Admin (Bước 7 - Phần 1 & 3).
 *
 * Phân quyền theo PHAN_QUYEN_WEB_ADMIN.md:
 * - ADMIN & CSKH: toàn quyền xem, ẩn/hiện đánh giá vi phạm và phát thông báo khuyến mãi/hỗ trợ.
 * - WAREHOUSE_MANAGER & CUSTOMER: bị chặn hoàn toàn (403 Forbidden).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'CSKH')")
public class AdminReviewNotifController {

    private final ReviewReturnService reviewReturnService;
    private final NotificationService notificationService;

    public AdminReviewNotifController(
            ReviewReturnService reviewReturnService,
            NotificationService notificationService
    ) {
        this.reviewReturnService = reviewReturnService;
        this.notificationService = notificationService;
    }

    // Xem toàn bộ đánh giá sách (kể cả đã ẩn) để kiểm duyệt
    @GetMapping("/reviews")
    public ApiResponse<List<AdminReviewDto>> getAdminReviews(
            @RequestParam(name = "biAn", required = false) Boolean biAn,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ApiResponse.ok(
                "Lấy danh sách đánh giá quản trị thành công",
                reviewReturnService.getAdminReviews(biAn, keyword)
        );
    }

    // Ẩn hoặc hiện lại bình luận vi phạm/spam (tự động tính lại điểm sao trung bình của sách)
    @PatchMapping("/reviews/{id}/toggle-hidden")
    public ApiResponse<AdminReviewDto> toggleReviewHidden(@PathVariable("id") String id) {
        AdminReviewDto updated = reviewReturnService.toggleReviewHidden(id);
        String msg = Boolean.TRUE.equals(updated.biAn())
                ? "Đã ẩn đánh giá vi phạm khỏi Mobile App"
                : "Đã hiển thị lại đánh giá trên Mobile App";
        return ApiResponse.ok(msg, updated);
    }

    // Phát sóng thông báo khuyến mãi / hệ thống tới toàn bộ tài khoản khách hàng
    @PostMapping("/notifications/broadcast")
    public ApiResponse<Map<String, Integer>> broadcastNotification(
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        int recipients = notificationService.broadcastToAllCustomers(request);
        return ApiResponse.ok(
                "Đã phát sóng thông báo tới " + recipients + " khách hàng",
                Map.of("soKhachHangNhan", recipients)
        );
    }
}
