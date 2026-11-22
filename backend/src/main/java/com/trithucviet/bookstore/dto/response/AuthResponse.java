package com.trithucviet.bookstore.dto.response;

/**
 * Kết quả trả về sau khi Đăng ký hoặc Đăng nhập thành công:
 * chứa JWT Bearer Token để client gắn vào header Authorization cho các request tiếp theo.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        UserDto user
) {
}
