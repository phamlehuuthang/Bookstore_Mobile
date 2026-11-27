package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Vui lòng nhập email tài khoản.")
        @Email(message = "Email không đúng định dạng.")
        String email
) {
}
