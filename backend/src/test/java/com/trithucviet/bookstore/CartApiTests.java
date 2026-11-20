package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Kiểm thử tích hợp toàn bộ API Giỏ hàng (Bước 4 - phần 2) trên dữ liệu thật của bookstore_db.
 * - @Transactional ở cấp class tự rollback mọi thay đổi sau từng test -> không làm bẩn dữ liệu mẫu.
 * - MockMvc gắn .apply(springSecurity()) để chạy đầy đủ JwtAuthenticationFilter và @PreAuthorize.
 * - Dữ liệu mẫu của nd_kh01: sach_011 (Macbeth, 330.000đ, SL 1, da_chon=true, tồn 97)
 *   và sach_026 (The Invisible Man, 248.000đ, SL 2, da_chon=true, tồn 74).
 */
@SpringBootTest
@Transactional
class CartApiTests {

    private static final String BASE = "/api/v1/cart";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private String customerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        customerToken = bearer("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);
    }

    private String bearer(String userId, String email, VaiTro vaiTro) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, email, vaiTro);
    }

    // --- Nhóm 1: Luồng nghiệp vụ thành công & tính toán giá / Flash Sale / Freeship ---

    @Test
    void xemGioHangBanDau_tinhDungTamTinhVaFreeship() throws Exception {
        mockMvc.perform(get(BASE).header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tongSoDauSach").value(2))
                .andExpect(jsonPath("$.data.tongSoLuongTrongGio").value(3))
                .andExpect(jsonPath("$.data.soMonDaChon").value(2))
                .andExpect(jsonPath("$.data.daChonTatCa").value(true))
                .andExpect(jsonPath("$.data.tamTinh").value(826000))
                .andExpect(jsonPath("$.data.phiVanChuyen").value(0))
                .andExpect(jsonPath("$.data.conThieuDeFreeship").value(0))
                .andExpect(jsonPath("$.data.tongDuKien").value(826000));
    }

    @Test
    void themSachFlashSaleVaCongDonSoLuong_apDungDungDonGiaFlashSale() throws Exception {
        // sach_007 đang Flash Sale: giaBan=345000, giaFlashSale=276000
        mockMvc.perform(post(BASE)
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sachId\":\"sach_007\",\"soLuong\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tongSoDauSach").value(3))
                .andExpect(jsonPath("$.data.tongSoLuongTrongGio").value(4))
                .andExpect(jsonPath("$.data.items[0].sachId").value("sach_007"))
                .andExpect(jsonPath("$.data.items[0].donGiaApDung").value(276000))
                .andExpect(jsonPath("$.data.items[0].thanhTien").value(276000))
                .andExpect(jsonPath("$.data.tamTinh").value(1102000));

        // Thêm tiếp sach_007 -> phải cộng dồn thành 2 cuốn (thanhTien = 552000)
        mockMvc.perform(post(BASE)
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sachId\":\"sach_007\",\"soLuong\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tongSoDauSach").value(3))
                .andExpect(jsonPath("$.data.tongSoLuongTrongGio").value(5))
                .andExpect(jsonPath("$.data.items[0].soLuong").value(2))
                .andExpect(jsonPath("$.data.items[0].thanhTien").value(552000))
                .andExpect(jsonPath("$.data.tamTinh").value(1378000));
    }

    @Test
    void capNhatSoLuongVaBoChonTungMon_tinhLaiTamTinhChinhXac() throws Exception {
        // Tăng sach_011 (330k) lên 3 cuốn -> 990k + sach_026 (496k) = 1.486.000đ
        mockMvc.perform(put(BASE + "/sach_011")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"soLuong\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tongSoLuongTrongGio").value(5))
                .andExpect(jsonPath("$.data.tamTinh").value(1486000));

        // Bỏ tick chọn sach_011 -> chỉ còn sach_026 (496.000đ) được tính vào tamTinh
        mockMvc.perform(put(BASE + "/sach_011")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"daChon\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.soMonDaChon").value(1))
                .andExpect(jsonPath("$.data.daChonTatCa").value(false))
                .andExpect(jsonPath("$.data.tamTinh").value(496000));
    }

    @Test
    void chonTatCaVaBoChonTatCa_phiShip0KhiKhongChonVa25kKhiDuoi300k() throws Exception {
        // Bỏ chọn tất cả -> tamTinh = 0, phiVanChuyen phải là 0đ (không tính 25k khi chưa chọn món nào)
        mockMvc.perform(patch(BASE + "/select-all")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"daChon\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.soMonDaChon").value(0))
                .andExpect(jsonPath("$.data.daChonTatCa").value(false))
                .andExpect(jsonPath("$.data.tamTinh").value(0))
                .andExpect(jsonPath("$.data.phiVanChuyen").value(0))
                .andExpect(jsonPath("$.data.conThieuDeFreeship").value(300000))
                .andExpect(jsonPath("$.data.tongDuKien").value(0));

        // Thêm 1 cuốn dưới 300k (sach_007 Flash Sale = 276.000đ, mặc định daChon=true) -> ship 25k, thiếu 24k để freeship
        mockMvc.perform(post(BASE)
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sachId\":\"sach_007\",\"soLuong\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.soMonDaChon").value(1))
                .andExpect(jsonPath("$.data.daChonTatCa").value(false))
                .andExpect(jsonPath("$.data.tamTinh").value(276000))
                .andExpect(jsonPath("$.data.phiVanChuyen").value(25000))
                .andExpect(jsonPath("$.data.conThieuDeFreeship").value(24000))
                .andExpect(jsonPath("$.data.tongDuKien").value(301000));

        // Bấm Chọn tất cả trở lại -> mọi món đều daChon=true
        mockMvc.perform(patch(BASE + "/select-all")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"daChon\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.soMonDaChon").value(3))
                .andExpect(jsonPath("$.data.daChonTatCa").value(true))
                .andExpect(jsonPath("$.data.phiVanChuyen").value(0));
    }

    @Test
    void xoaMonKhoiGio_thanhCongVaGoiLan2Tra404() throws Exception {
        mockMvc.perform(delete(BASE + "/sach_011").header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tongSoDauSach").value(1))
                .andExpect(jsonPath("$.data.items[*].sachId", not(hasItem("sach_011"))))
                .andExpect(jsonPath("$.data.tamTinh").value(496000));

        mockMvc.perform(delete(BASE + "/sach_011").header("Authorization", customerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // --- Nhóm 2: Kiểm tra ràng buộc tồn kho, validation Phương án B (mức tối thiểu 1) & 404 ---

    @Test
    void themQuaTonKho_tra400KemThongBaoTonKho() throws Exception {
        // sach_001 tồn kho = 27
        mockMvc.perform(post(BASE)
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sachId\":\"sach_001\",\"soLuong\":28}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Sách 'Alice's Adventures in Wonderland' chỉ còn 27 cuốn trong kho."));
    }

    @Test
    void suaSoLuongVeKhong_tra400TheoPhuongAnB() throws Exception {
        mockMvc.perform(put(BASE + "/sach_011")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"soLuong\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.soLuong").value("Số lượng tối thiểu là 1 cuốn."));
    }

    @Test
    void capNhatBodyRong_tra400() throws Exception {
        mockMvc.perform(put(BASE + "/sach_011")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void themSachKhongTonTai_tra404() throws Exception {
        mockMvc.perform(post(BASE)
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sachId\":\"sach_999\",\"soLuong\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy cuốn sách có mã sach_999."));
    }

    // --- Nhóm 3: Bảo mật & Phân quyền (401 / 403) ---

    @Test
    void khongCoTokenHoacTokenSai_tra401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE).header("Authorization", "Bearer token-gia-mao"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nhanSuAdmin_khongDuocDungGioHang_tra403() throws Exception {
        mockMvc.perform(get(BASE).header("Authorization", bearer("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void khachBiKhoa_tra403() throws Exception {
        mockMvc.perform(get(BASE).header("Authorization", bearer("nd_kh09", "kiet.do@gmail.com", VaiTro.CUSTOMER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ CSKH để được hỗ trợ."));
    }
}
