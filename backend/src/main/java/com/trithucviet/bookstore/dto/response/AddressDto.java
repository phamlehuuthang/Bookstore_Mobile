package com.trithucviet.bookstore.dto.response;

/**
 * Thông tin 1 địa chỉ nhận hàng trả về cho Mobile App (Hồ sơ & Thanh toán).
 * Có thêm trường diaChiDayDu đã ghép sẵn để frontend hiển thị trực tiếp mà không phải nối chuỗi lại nhiều nơi.
 */
public record AddressDto(
        String id,
        String hoTenNguoiNhan,
        String soDienThoai,
        String tinhThanh,
        String quanHuyen,
        String phuongXa,
        String diaChiChiTiet,
        String diaChiDayDu,
        Boolean macDinh
) {
}
