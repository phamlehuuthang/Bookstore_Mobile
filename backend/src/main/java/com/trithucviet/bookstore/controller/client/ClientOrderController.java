package com.trithucviet.bookstore.controller.client;

import com.trithucviet.bookstore.dto.request.CancelOrderRequest;
import com.trithucviet.bookstore.dto.request.CreateOrderRequest;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.OrderDto;
import com.trithucviet.bookstore.entity.enums.TrangThaiDon;
import com.trithucviet.bookstore.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Đặt hàng, Xem đơn hàng & Hủy đơn hàng cho Khách hàng Mobile App (Bước 6 - Phần 1 & 2).
 *
 * Bảo mật & kỹ thuật:
 * - @PreAuthorize("hasRole('CUSTOMER')"): chỉ tài khoản khách hàng được phép đặt, xem và hủy đơn cá nhân.
 * - Luôn lấy nguoiDungId từ authentication.getName() (JWT) để chống IDOR.
 * - Ghi tường minh tên tham số trong @PathVariable("id") và @RequestParam(name = ...) để chạy ổn định trên IDE.
 */
@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class ClientOrderController {

    private final OrderService orderService;

    public ClientOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Đặt hàng từ các sách đã tick chọn trong giỏ hàng
    @PostMapping
    public ApiResponse<OrderDto> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return ApiResponse.ok(
                "Đặt hàng thành công",
                orderService.createOrder(authentication.getName(), request)
        );
    }

    // Xem danh sách đơn hàng của khách (có thể lọc theo tab trạng thái)
    @GetMapping
    public ApiResponse<List<OrderDto>> getCustomerOrders(
            Authentication authentication,
            @RequestParam(name = "trangThai", required = false) TrangThaiDon trangThai
    ) {
        return ApiResponse.ok(
                "Lấy danh sách đơn hàng thành công",
                orderService.getCustomerOrders(authentication.getName(), trangThai)
        );
    }

    // Xem chi tiết 1 đơn hàng + danh sách sách + Timeline vận chuyển
    @GetMapping("/{id}")
    public ApiResponse<OrderDto> getCustomerOrderDetail(
            Authentication authentication,
            @PathVariable("id") String id
    ) {
        return ApiResponse.ok(
                "Lấy chi tiết đơn hàng thành công",
                orderService.getCustomerOrderDetail(authentication.getName(), id)
        );
    }

    // Khách hủy đơn hàng khi đơn đang ở trạng thái CHO_XAC_NHAN
    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderDto> cancelCustomerOrder(
            Authentication authentication,
            @PathVariable("id") String id,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        return ApiResponse.ok(
                "Đã hủy đơn hàng và hoàn dữ liệu kho",
                orderService.cancelCustomerOrder(authentication.getName(), id, request)
        );
    }

    /**
     * Phương án 1: Khách hàng chủ động bấm nút "Đã nhận được hàng" trên Mobile App khi đơn đang DANG_GIAO.
     * Giúp hoàn tất đơn sang DA_GIAO (và DA_THANH_TOAN nếu COD), mở khóa ngay nút Đánh giá & Đổi trả
     * mà phía Admin không cần bấm xác nhận thủ công.
     */
    @PostMapping("/{id}/confirm-received")
    public ApiResponse<OrderDto> confirmOrderReceived(
            Authentication authentication,
            @PathVariable("id") String id
    ) {
        return ApiResponse.ok(
                "Xác nhận đã nhận hàng thành công",
                orderService.confirmOrderReceived(authentication.getName(), id)
        );
    }
}
