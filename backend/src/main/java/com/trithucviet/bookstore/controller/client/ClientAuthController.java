package com.trithucviet.bookstore.controller.client;

import com.trithucviet.bookstore.dto.request.ChangePasswordRequest;
import com.trithucviet.bookstore.dto.request.ForgotPasswordRequest;
import com.trithucviet.bookstore.dto.request.LoginRequest;
import com.trithucviet.bookstore.dto.request.RegisterRequest;
import com.trithucviet.bookstore.dto.request.ResetPasswordRequest;
import com.trithucviet.bookstore.dto.request.UpdateProfileRequest;
import com.trithucviet.bookstore.dto.response.ApiResponse;
import com.trithucviet.bookstore.dto.response.AuthResponse;
import com.trithucviet.bookstore.dto.response.UserDto;
import com.trithucviet.bookstore.exception.BusinessException;
import com.trithucviet.bookstore.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Xác thực & Hồ sơ cá nhân (Bước 2 - phần 1):
 * - Mobile App: Đăng ký (/api/v1/auth/register), Đăng nhập (/api/v1/auth/login),
 *   Xem hồ sơ (/api/v1/auth/me), Cập nhật hồ sơ (/api/v1/auth/profile), Đổi mật khẩu (/api/v1/auth/password).
 * - Web Admin: Đăng nhập nội bộ (/api/admin/auth/login).
 *
 * Lưu ý kỹ thuật quan trọng:
 * - SecurityConfig đang để permitAll() cho /api/v1/auth/** nhằm cho phép register/login không cần token.
 *   Vì vậy 3 API /me, /profile, /password kiểm tra AnonymousAuthenticationToken trực tiếp ở đây
 *   để trả đúng 401 Unauthorized khi thiếu token mà KHÔNG phải chỉnh sửa file dùng chung SecurityConfig.java.
 */
@RestController
public class ClientAuthController {

    private final AuthService authService;

    public ClientAuthController(AuthService authService) {
        this.authService = authService;
    }

    // Đăng ký tài khoản khách hàng mới trên Mobile App (luôn gán vai trò CUSTOMER)
    @PostMapping("/api/v1/auth/register")
    public ApiResponse<AuthResponse> registerCustomer(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Đăng ký tài khoản thành công", authService.registerCustomer(request));
    }

    // Đăng nhập Mobile App (chặn tài khoản nhân sự ADMIN/WAREHOUSE_MANAGER/CSKH và tài khoản bị khóa)
    @PostMapping("/api/v1/auth/login")
    public ApiResponse<AuthResponse> loginMobile(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Đăng nhập thành công", authService.loginMobile(request));
    }

    // Đăng nhập Web Admin Dashboard (chỉ cho ADMIN, WAREHOUSE_MANAGER, CSKH; chặn CUSTOMER)
    @PostMapping("/api/admin/auth/login")
    public ApiResponse<AuthResponse> loginAdmin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Đăng nhập Web Admin thành công", authService.loginAdmin(request));
    }

    // Lấy thông tin hồ sơ tài khoản đang đăng nhập theo userId trong JWT
    @GetMapping("/api/v1/auth/me")
    public ApiResponse<UserDto> getCurrentUser(Authentication authentication) {
        return ApiResponse.ok(
                "Lấy thông tin tài khoản thành công",
                authService.getCurrentUser(requireAuthenticatedUserId(authentication))
        );
    }

    // Cập nhật họ tên & số điện thoại cho tài khoản đang đăng nhập
    @PutMapping("/api/v1/auth/profile")
    public ApiResponse<UserDto> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.ok(
                "Cập nhật hồ sơ thành công",
                authService.updateProfile(requireAuthenticatedUserId(authentication), request)
        );
    }

    // Đổi mật khẩu cá nhân (yêu cầu nhập đúng mật khẩu hiện tại và chuyển lưu trữ sang BCrypt)
    @PutMapping("/api/v1/auth/password")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(requireAuthenticatedUserId(authentication), request);
        return ApiResponse.ok("Đổi mật khẩu thành công", null);
    }

    // Quên mật khẩu - Bước 1: gửi mã OTP 6 chữ số (công khai)
    @PostMapping("/api/v1/auth/forgot-password")
    public ApiResponse<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.ok("Đã tạo mã xác thực OTP khôi phục mật khẩu", authService.requestPasswordResetOtp(request));
    }

    // Quên mật khẩu - Bước 2: xác thực OTP & đặt mật khẩu mới (công khai)
    @PostMapping("/api/v1/auth/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPasswordWithOtp(request);
        return ApiResponse.ok("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.", null);
    }

    private String requireAuthenticatedUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để sử dụng chức năng này.");
        }
        return authentication.getName();
    }
}
