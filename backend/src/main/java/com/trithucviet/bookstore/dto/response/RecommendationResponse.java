package com.trithucviet.bookstore.dto.response;

import java.util.List;

/**
 * Kết quả Đề xuất Sách Cá nhân hóa trên Trang chủ Mobile App (Bước 8 Người A).
 * - reasonLabel: dòng giải thích lý do gợi ý (VD: "Theo sở thích: Văn học & Tiểu thuyết • Trinh thám & Bí ẩn").
 * - topCategories: danh sách tối đa 3 tên chủ đề có điểm Affinity Score cao nhất.
 * - books: tối đa 6 cuốn sách đã pha trộn theo tỷ lệ 3:2:1 và loại trừ sách khách đã lưu/bỏ giỏ/mua.
 */
public record RecommendationResponse(
        String reasonLabel,
        List<String> topCategories,
        List<BookDto> books
) {
}
