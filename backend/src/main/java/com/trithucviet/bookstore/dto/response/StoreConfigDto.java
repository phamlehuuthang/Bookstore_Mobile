package com.trithucviet.bookstore.dto.response;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO Cấu hình Cửa hàng lưu bền vững trên PostgreSQL (`cau_hinh_cua_hang` — Phần B1).
 */
public record StoreConfigDto(
        String id,
        @NotBlank(message = "Tên cửa hàng không được để trống.") String tenCuaHang,
        String khauHieu,
        String logoUrl,
        @NotBlank(message = "Hotline CSKH không được để trống.") String hotline,
        @NotBlank(message = "Email hỗ trợ không được để trống.")
        @Email(message = "Email hỗ trợ không đúng định dạng.") String emailHoTro,
        @NotBlank(message = "Địa chỉ trụ sở không được để trống.") String diaChiTruSo,
        @NotNull(message = "Phí ship mặc định không được để trống.")
        @Min(value = 0, message = "Phí ship không được âm.") BigDecimal phiShipMacDinh,
        @NotNull(message = "Ngưỡng Freeship không được để trống.")
        @Min(value = 0, message = "Ngưỡng Freeship không được âm.") BigDecimal mienPhiShipTu,
        @NotNull(message = "Ngưỡng cảnh báo tồn kho không được để trống.")
        @Min(value = 1, message = "Ngưỡng cảnh báo tồn kho tối thiểu là 1.") Integer nguongCanhBaoTonKho,
        @NotBlank(message = "Tên ngân hàng thụ hưởng không được để trống.") String nganHangTen,
        @NotBlank(message = "Số tài khoản ngân hàng không được để trống.") String nganHangSoTk,
        @NotBlank(message = "Tên chủ tài khoản không được để trống.") String nganHangChuTk
) {
}
