package com.trithucviet.bookstore.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dữ liệu tổng hợp cho Dashboard Web Admin (Bước 8) có lọc theo phân quyền RBAC:
 * - ADMIN: nhận đầy đủ chỉ số tài chính (totalRevenue, revenueGrowth, revenueByDay/Month/Year) + kho + đơn hàng.
 * - WAREHOUSE_MANAGER: các trường tài chính (totalRevenue, revenueGrowth, revenueBy*) = null; nhận lowStockBooks & topSellingBooks.
 * - CSKH: các trường tài chính = null; nhận totalOrders, newCustomers, pendingOrdersCount, pendingReturnsCount.
 */
public record AdminDashboardDto(
        BigDecimal totalRevenue,
        Double revenueGrowth,
        Long totalOrders,
        Double ordersGrowth,
        Long newCustomers,
        Double customersGrowth,
        Long pendingOrdersCount,
        Long pendingReturnsCount,
        Integer lowStockCount,
        Integer lowStockThreshold,
        List<RevenuePointDto> revenueByDay,
        List<RevenuePointDto> revenueByMonth,
        List<RevenuePointDto> revenueByYear,
        List<DashboardBookItemDto> topSellingBooks,
        List<DashboardBookItemDto> lowStockBooks
) {
}
