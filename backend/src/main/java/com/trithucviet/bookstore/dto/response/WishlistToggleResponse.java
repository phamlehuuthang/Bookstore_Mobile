package com.trithucviet.bookstore.dto.response;

/**
 * Kết quả trả về sau khi thả tim / bỏ tim, xóa trực tiếp hoặc kiểm tra trạng thái yêu thích của 1 cuốn sách.
 *
 * - daYeuThich: Mobile dựa vào cờ boolean này để tô đỏ hoặc làm rỗng icon trái tim trên màn Chi tiết sách.
 * - tongSoSachYeuThich: Cập nhật ngay số đếm trên badge "Yêu thích (N)" ở tab Hồ sơ mà không cần gọi lại GET /wishlists.
 */
public record WishlistToggleResponse(
        String sachId,
        boolean daYeuThich,
        long tongSoSachYeuThich
) {
}
