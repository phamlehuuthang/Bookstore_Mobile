package com.trithucviet.bookstore.dto.response;

import java.math.BigDecimal;

/**
 * Kết quả kiểm tra điều kiện và tính thử mức giảm của 1 Voucher (POST /api/v1/vouchers/validate).
 *
 * Tách rõ 2 con số tạm tính để minh bạch khi dùng mã theo danh mục (như CONGNGHE25):
 * - tamTinh: tổng tiền hàng toàn đơn đang xét (dùng để xét điều kiện Freeship >= 300.000đ).
 * - tamTinhApDungVoucher: phần tiền hợp lệ được hưởng mã (bằng tamTinh nếu mã toàn shop; hoặc chỉ tính riêng
 *   tổng tiền các cuốn sách thuộc danh mục mà mã quy định để đối chiếu donToiThieu và nhân % giảm).
 */
public record VoucherValidationResponse(
        VoucherDto voucher,
        BigDecimal tamTinh,
        BigDecimal tamTinhApDungVoucher,
        BigDecimal phiVanChuyen,
        BigDecimal tienGiamGia,
        BigDecimal tongThanhToanDuKien
) {
}
