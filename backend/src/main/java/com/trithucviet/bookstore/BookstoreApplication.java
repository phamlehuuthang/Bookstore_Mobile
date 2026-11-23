package com.trithucviet.bookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Điểm khởi chạy ứng dụng Backend Spring Boot Nhà sách Tri Thức Việt.
 * - @EnableScheduling kích hoạt các tác vụ chạy ngầm định kỳ (như OrderAutoCompleteScheduler
 *   tự động chốt hoàn tất đơn DANG_GIAO quá hạn khi bật cấu hình).
 */
@SpringBootApplication
@EnableScheduling
public class BookstoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookstoreApplication.class, args);
	}

}
