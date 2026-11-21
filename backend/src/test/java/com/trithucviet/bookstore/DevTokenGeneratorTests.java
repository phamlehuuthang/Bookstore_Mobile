package com.trithucviet.bookstore;

import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Công cụ hỗ trợ test bằng Postman khi CHƯA có API Đăng nhập (Bước 2 của Người A).
 * Chạy: .\gradlew.bat test --tests "com.trithucviet.bookstore.DevTokenGeneratorTests"
 * Kết quả: file backend/build/dev-tokens.txt chứa JWT (hạn 24 giờ) của các tài khoản mẫu.
 */
@SpringBootTest
class DevTokenGeneratorTests {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void sinhTokenChoPostman() throws Exception {
        String content = """
                # JWT dung de test bang Postman (han 24 gio) - dan vao tab Authorization > Bearer Token
                CUSTOMER   nd_kh01 (Nguyen Huu Thang - dang yeu thich sach_001, sach_016):
                %s

                CUSTOMER   nd_kh09 (Do Tuan Kiet - TAI KHOAN BI KHOA):
                %s

                ADMIN      nd_admin (Pham Minh Quan):
                %s

                CSKH       nd_cskh (Le Ngoc Han - chi xem voucher, bi chan tao/sua/tat):
                %s

                WAREHOUSE  nd_kho (Tran Quoc Dat - bi chan hoan toan khoi voucher):
                %s
                """.formatted(
                jwtTokenProvider.generateToken("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER),
                jwtTokenProvider.generateToken("nd_kh09", "kiet.do@gmail.com", VaiTro.CUSTOMER),
                jwtTokenProvider.generateToken("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN),
                jwtTokenProvider.generateToken("nd_cskh", "cskh@trithucviet.vn", VaiTro.CSKH),
                jwtTokenProvider.generateToken("nd_kho", "kho@trithucviet.vn", VaiTro.WAREHOUSE_MANAGER)
        );
        Path output = Path.of("build", "dev-tokens.txt");
        Files.createDirectories(output.getParent());
        Files.writeString(output, content, StandardCharsets.UTF_8);
    }
}
