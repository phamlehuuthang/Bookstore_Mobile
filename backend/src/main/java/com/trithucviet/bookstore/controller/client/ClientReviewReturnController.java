package com.trithucviet.bookstore.controller.client;

import com.trithucviet.bookstore.dto.request.CreateReturnRequest;
import com.trithucviet.bookstore.dto.request.CreateReviewRequest;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.ReturnRequestDto;
import com.trithucviet.bookstore.dto.response.ReviewDto;
import com.trithucviet.bookstore.service.ReviewReturnService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Đánh giá sách & Gửi yêu cầu Đổi/Trả hàng cho Khách hàng Mobile App (Bước 7 - Phần 1 & 2).
 * - Chỉ cho phép vai trò CUSTOMER gọi và luôn lấy nguoiDungId từ JWT để chống IDOR.
 */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('CUSTOMER')")
public class ClientReviewReturnController {

    private final ReviewReturnService reviewReturnService;

    public ClientReviewReturnController(ReviewReturnService reviewReturnService) {
        this.reviewReturnService = reviewReturnService;
    }

    // Khách gửi đánh giá 1-5 sao kèm nhận xét & ảnh cho sách trong đơn đã giao (DA_GIAO)
    @PostMapping("/reviews")
    public ApiResponse<ReviewDto> submitReview(
            Authentication authentication,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return ApiResponse.ok(
                "Gửi đánh giá sách thành công",
                reviewReturnService.submitReview(authentication.getName(), request)
        );
    }

    // Khách gửi yêu cầu Đổi/Trả hàng cho đơn đã giao (mỗi đơn tối đa 1 yêu cầu)
    @PostMapping("/returns")
    public ApiResponse<ReturnRequestDto> submitReturnRequest(
            Authentication authentication,
            @Valid @RequestBody CreateReturnRequest request
    ) {
        return ApiResponse.ok(
                "Đã gửi yêu cầu đổi trả thành công",
                reviewReturnService.submitReturnRequest(authentication.getName(), request)
        );
    }
}
