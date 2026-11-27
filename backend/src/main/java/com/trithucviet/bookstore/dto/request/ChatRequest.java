package com.trithucviet.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Payload gửi câu hỏi tới Tư vấn viên AI (POST /api/v1/chat/ask - Bước 10).
 * - history: tối đa 8 lượt tin nhắn gần nhất để AI hiểu ngữ cảnh hội thoại (VD: "cuốn đó", "xác nhận đặt").
 */
public record ChatRequest(
        @NotBlank(message = "Nội dung câu hỏi không được để trống")
        @Size(max = 1000, message = "Nội dung tin nhắn tối đa 1000 ký tự")
        String message,

        List<HistoryMessage> history
) {
    public record HistoryMessage(
            String sender,
            String text
    ) {
    }
}
