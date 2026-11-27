package com.trithucviet.bookstore.controller.client;

import com.trithucviet.bookstore.dto.request.TrackBehaviorRequest;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.RecommendationResponse;
import com.trithucviet.bookstore.dto.response.TrackBehaviorResponse;
import com.trithucviet.bookstore.service.RecommendationTrackingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Theo dõi Hành vi Người dùng & Đề xuất Sách Cá nhân hóa trên Mobile App (Bước 8 Người A).
 *
 * - POST /api/v1/tracking/event: yêu cầu đăng nhập tài khoản CUSTOMER.
 * - GET /api/v1/books/recommendations: mở công khai (permitAll trong SecurityConfig);
 *   nếu khách đã đăng nhập thì tính đề xuất cá nhân hóa từ bảng hanh_vi_nguoi_dung,
 *   nếu khách vãng lai thì trả về Top 6 sách bán chạy theo xu hướng.
 */
@RestController
public class ClientTrackingController {

    private final RecommendationTrackingService recommendationTrackingService;

    public ClientTrackingController(RecommendationTrackingService recommendationTrackingService) {
        this.recommendationTrackingService = recommendationTrackingService;
    }

    @PostMapping("/api/v1/tracking/event")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<TrackBehaviorResponse> trackEvent(
            Authentication authentication,
            @Valid @RequestBody TrackBehaviorRequest request
    ) {
        TrackBehaviorResponse res = recommendationTrackingService.trackEvent(authentication.getName(), request);
        String msg = res.ignored()
                ? "Bỏ qua sự kiện xem dưới 3 giây"
                : "Đã ghi nhận hành vi người dùng thành công";
        return ApiResponse.ok(msg, res);
    }

    @GetMapping("/api/v1/books/recommendations")
    public ApiResponse<RecommendationResponse> getRecommendations(Authentication authentication) {
        String userId = (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal()))
                ? authentication.getName()
                : null;
        return ApiResponse.ok(
                "Lấy danh sách sách đề xuất thành công",
                recommendationTrackingService.getRecommendations(userId)
        );
    }
}
