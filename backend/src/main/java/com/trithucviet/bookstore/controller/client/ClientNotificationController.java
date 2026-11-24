package com.trithucviet.bookstore.controller.client;

import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.NotificationDto;
import com.trithucviet.bookstore.service.NotificationService;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Thông báo đẩy cho Khách hàng Mobile App (Bước 7 - Phần 3).
 * - Ghi tường minh @PathVariable("id") để chạy ổn định khi nhấn Run trực tiếp từ IDE.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("hasRole('CUSTOMER')")
public class ClientNotificationController {

    private final NotificationService notificationService;

    public ClientNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Lấy toàn bộ thông báo cá nhân + thông báo chung toàn shop của khách đang đăng nhập
    @GetMapping
    public ApiResponse<List<NotificationDto>> getNotifications(Authentication authentication) {
        return ApiResponse.ok(
                "Lấy danh sách thông báo thành công",
                notificationService.getCustomerNotifications(authentication.getName())
        );
    }

    // Đánh dấu đã đọc 1 thông báo
    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationDto> markAsRead(
            Authentication authentication,
            @PathVariable("id") String id
    ) {
        return ApiResponse.ok(
                "Đã đánh dấu thông báo là đã đọc",
                notificationService.markAsRead(authentication.getName(), id)
        );
    }

    // Đánh dấu đã đọc toàn bộ thông báo cá nhân chưa đọc
    @PatchMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllAsRead(Authentication authentication) {
        int updatedCount = notificationService.markAllAsRead(authentication.getName());
        return ApiResponse.ok(
                "Đã đánh dấu tất cả thông báo là đã đọc",
                Map.of("soThongBaoDaDoc", updatedCount)
        );
    }
}
