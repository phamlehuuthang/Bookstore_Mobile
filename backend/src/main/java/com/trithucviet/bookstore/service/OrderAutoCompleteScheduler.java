package com.trithucviet.bookstore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tác vụ chạy ngầm định kỳ (Phương án 2): tự động hoàn tất các đơn hàng DANG_GIAO quá hạn xác nhận.
 *
 * Cơ chế hoạt động:
 * - Cứ mỗi `scan-interval-ms` (mặc định 60 giây), Scheduler kiểm tra cờ `app.order.auto-complete.enabled`.
 * - Nếu bật (`true`), hệ thống gọi `orderService.autoCompleteOverdueShippingOrders(delayMinutes)`
 *   để chuyển các đơn đã giao quá thời hạn chờ sang `DA_GIAO`.
 * - Mặc định đặt `enabled=false` để giữ nguyên trạng thái các đơn mẫu (như `dh_03`) khi chạy test hoặc demo;
 *   khi muốn trình diễn tính năng tự chốt đơn không cần bấm tay, chỉ cần đổi `enabled=true` trong application.properties.
 */
@Component
public class OrderAutoCompleteScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoCompleteScheduler.class);

    private final OrderService orderService;
    private final boolean autoCompleteEnabled;
    private final long delayMinutes;

    public OrderAutoCompleteScheduler(
            OrderService orderService,
            @Value("${app.order.auto-complete.enabled:false}") boolean autoCompleteEnabled,
            @Value("${app.order.auto-complete.delay-minutes:4320}") long delayMinutes
    ) {
        this.orderService = orderService;
        this.autoCompleteEnabled = autoCompleteEnabled;
        this.delayMinutes = delayMinutes;
    }

    @Scheduled(
            fixedDelayString = "${app.order.auto-complete.scan-interval-ms:60000}",
            initialDelayString = "${app.order.auto-complete.initial-delay-ms:60000}"
    )
    public void runAutoCompleteJob() {
        if (!autoCompleteEnabled) {
            return;
        }
        int completedCount = orderService.autoCompleteOverdueShippingOrders(delayMinutes);
        if (completedCount > 0) {
            log.info("Auto-complete scheduler đã tự động hoàn tất {} đơn hàng quá hạn.", completedCount);
        }
    }
}
