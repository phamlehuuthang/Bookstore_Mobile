package com.trithucviet.bookstore.dto.response;

import java.math.BigDecimal;

/**
 * Một mốc thống kê doanh thu / số đơn / lợi nhuận gộp trên biểu đồ Dashboard & báo cáo CSV.
 */
public record RevenuePointDto(
        String label,
        BigDecimal revenue,
        Long orders,
        BigDecimal profit
) {
}
