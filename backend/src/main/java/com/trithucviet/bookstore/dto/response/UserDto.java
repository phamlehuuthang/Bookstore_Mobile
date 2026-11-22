package com.trithucviet.bookstore.dto.response;

import com.trithucviet.bookstore.entity.enums.VaiTro;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thông tin tài khoản công khai trả về cho Mobile App và Web Admin.
 * Tuyệt đối không đưa trường matKhauMaHoa vào DTO này để tránh lộ chuỗi băm mật khẩu ra API.
 */
public record UserDto(
        String id,
        String hoTen,
        String email,
        String soDienThoai,
        VaiTro vaiTro,
        Boolean biKhoa,
        BigDecimal tongChiTieu,
        Integer soDonHang,
        LocalDateTime ngayTao
) {
}
