package com.trithucviet.bookstore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Payload tạo Phiếu Nhập Kho mới từ Web Admin (ADMIN & WAREHOUSE_MANAGER - Bước 8).
 */
public record CreateInventoryReceiptRequest(
        @NotBlank(message = "Tên nhà cung cấp không được để trống")
        @Size(max = 200, message = "Tên nhà cung cấp tối đa 200 ký tự")
        String tenNhaCungCap,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String ghiChu,

        @NotEmpty(message = "Phiếu nhập kho phải có ít nhất 1 đầu sách")
        @Valid
        List<ItemRequest> items
) {
    public record ItemRequest(
            @NotBlank(message = "Mã sách không được để trống")
            String sachId,

            @NotNull(message = "Số lượng nhập không được để trống")
            @Min(value = 1, message = "Số lượng nhập phải lớn hơn 0")
            Integer soLuong,

            @NotNull(message = "Giá nhập không được để trống")
            @DecimalMin(value = "0", message = "Giá nhập không được âm")
            BigDecimal giaNhap
    ) {
    }
}
