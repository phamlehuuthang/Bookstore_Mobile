package com.trithucviet.bookstore.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Toàn bộ trạng thái Giỏ hàng & Bảng tạm tính chi phí của khách hàng (Bước 4 - phần 2).
 *
 * Mọi API thao tác giỏ hàng (GET, POST, PUT, PATCH select-all, DELETE) đều trả về DTO này để Mobile
 * cập nhật đồng bộ danh sách món, badge tổng số lượng và tiền tạm tính/phí ship chỉ trong 1 lần gọi.
 *
 * Lưu ý: tongDuKien = tamTinh + phiVanChuyen (chưa bao gồm tienGiamGia từ Voucher, vì Voucher sẽ được
 * kiểm tra và trừ ở Phần 3 - ClientVoucherController).
 */
public record CartResponseDto(
        List<CartItemDto> items,
        // Tổng số đầu sách khác nhau trong giỏ (hiển thị "Chọn tất cả (N đầu sách)")
        int tongSoDauSach,
        // Tổng số cuốn sách cộng dồn trong giỏ (hiển thị trên badge icon Giỏ hàng)
        int tongSoLuongTrongGio,
        // Số đầu sách đang được tick chọn để chuẩn bị thanh toán
        int soMonDaChon,
        // Tổng số cuốn đang được tick chọn
        int tongSoLuongDaChon,
        // Cờ bật nút "Chọn tất cả": true khi giỏ không rỗng và mọi món đều đang daChon = true
        boolean daChonTatCa,
        // Tổng (donGiaApDung * soLuong) của CHỈ những món đang được tick chọn (daChon = true)
        BigDecimal tamTinh,
        // 0đ nếu chưa tick chọn món nào hoặc tamTinh >= 300.000đ; ngược lại là 25.000đ
        BigDecimal phiVanChuyen,
        // Mốc đơn hàng được miễn phí vận chuyển tiêu chuẩn (300.000đ)
        BigDecimal nguongMienPhiShip,
        // Số tiền cần mua thêm để đạt Freeship (= max(0, 300.000 - tamTinh))
        BigDecimal conThieuDeFreeship,
        // Tổng dự kiến trước voucher (= tamTinh + phiVanChuyen)
        BigDecimal tongDuKien
) {

    /**
     * Chi tiết từng dòng sách trong giỏ hàng.
     * donGiaApDung tuân thủ Quy tắc 2.2: ưu tiên giaFlashSale nếu dangFlashSale = true và giaFlashSale > 0.
     */
    public record CartItemDto(
            String sachId,
            String tieuDe,
            String tenTacGia,
            String tenDanhMuc,
            String anhBiaUrl,
            BigDecimal giaBan,
            BigDecimal giaGoc,
            Boolean dangFlashSale,
            BigDecimal giaFlashSale,
            BigDecimal donGiaApDung,
            Integer soLuong,
            BigDecimal thanhTien,
            Integer tonKho,
            Boolean hienThi,
            Boolean daChon,
            LocalDateTime ngayCapNhat
    ) {
    }
}
