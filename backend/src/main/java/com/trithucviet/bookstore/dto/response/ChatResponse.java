package com.trithucviet.bookstore.dto.response;

import java.util.List;

/**
 * Phản hồi từ Tư vấn viên AI đa năng (POST /api/v1/chat/ask - Bước 10 nâng cấp).
 * - reply: câu trả lời tiếng Việt hiển thị trong bong bóng chat.
 * - suggestedBooks: danh sách sách thật từ DB để Mobile vẽ thẻ sách kèm nút Xem chi tiết / Thêm giỏ / Đặt ngay.
 * - cartUpdated: true khi AI vừa thao tác giỏ hàng (thêm / sửa số lượng / xóa / đặt giỏ) -> Mobile tự refresh giỏ.
 * - wishlistUpdated: true khi AI vừa thêm/bỏ sách yêu thích -> Mobile tự refresh Tủ sách yêu thích.
 * - notificationsUpdated: true khi AI vừa đặt/hủy đơn tạo thông báo mới -> Mobile tự refresh chuông thông báo.
 * - createdOrderCode: mã đơn hàng (VD: DH-XXXXXX) nếu AI vừa tạo đơn thành công cho khách.
 * - dynamicSuggestions: danh sách chip gợi ý hành động tiếp theo thông minh theo ngữ cảnh hội thoại.
 * - providerMode: "GEMINI" hoặc "SMART_FALLBACK".
 */
public record ChatResponse(
        String reply,
        List<BookDto> suggestedBooks,
        boolean cartUpdated,
        boolean wishlistUpdated,
        boolean notificationsUpdated,
        String createdOrderCode,
        List<String> dynamicSuggestions,
        String providerMode
) {
    public ChatResponse(
            String reply,
            List<BookDto> suggestedBooks,
            boolean cartUpdated,
            String createdOrderCode,
            String providerMode
    ) {
        this(reply, suggestedBooks, cartUpdated, false, createdOrderCode != null, createdOrderCode, List.of(), providerMode);
    }
}
