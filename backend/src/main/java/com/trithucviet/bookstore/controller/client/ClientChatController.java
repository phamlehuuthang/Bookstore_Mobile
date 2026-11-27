package com.trithucviet.bookstore.controller.client;

import com.trithucviet.bookstore.ai.AdvisorChatService;
import com.trithucviet.bookstore.dto.request.ChatRequest;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cổng REST API Hộp thoại Tư vấn viên AI trên Mobile App (Bước 10).
 *
 * - Mở công khai (permitAll trong SecurityConfig) để khách vãng lai vẫn hỏi tư vấn sách,
 *   xem tóm tắt sách, hỏi voucher và chính sách vận chuyển/đổi trả.
 * - Nếu khách đã đăng nhập (có JWT CUSTOMER hợp lệ), tự truyền nguoiDungId xuống AdvisorChatService
 *   để mở khóa các công cụ thêm giỏ hàng (addBookToCustomerCart) và đặt đơn COD trực tiếp (placeOrderForCustomer).
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ClientChatController {

    private final AdvisorChatService advisorChatService;

    public ClientChatController(AdvisorChatService advisorChatService) {
        this.advisorChatService = advisorChatService;
    }

    @PostMapping("/ask")
    public ApiResponse<ChatResponse> askAdvisor(
            Authentication authentication,
            @Valid @RequestBody ChatRequest request
    ) {
        String customerId = resolveCustomerId(authentication);
        return ApiResponse.ok(
                "Tư vấn viên AI phản hồi thành công",
                advisorChatService.ask(customerId, request)
        );
    }

    private String resolveCustomerId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        boolean isCustomer = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority()));
        return isCustomer ? authentication.getName() : null;
    }
}
