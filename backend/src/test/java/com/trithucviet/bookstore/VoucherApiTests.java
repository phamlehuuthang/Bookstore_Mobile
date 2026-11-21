package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import jakarta.persistence.EntityManager;
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
 * Kiểm thử tích hợp toàn bộ API Voucher phía Mobile (Bước 4 - phần 2 / Mục 5.2) trên dữ liệu thật bookstore_db.
 * - @Transactional tự rollback mọi thao tác thêm giỏ phụ để giữ nguyên trạng dữ liệu mẫu.
 * - Kiểm tra đầy đủ: mã công khai /active, áp mã theo giỏ thật, áp mã theo tamTinh thủ công,
 *   lọc đúng danh mục cha (dm_cong_nghe) + danh mục con (dm_kien_truc_pm) cho CONGNGHE25, và các lỗi 400/401/403/404.
 */
@SpringBootTest
@Transactional
class VoucherApiTests {

    private static final String BASE = "/api/v1/vouchers";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private String customerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        customerToken = bearer("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);

        // Cô lập giỏ hàng nd_kh01 trong transaction test (tự rollback sau mỗi test)
        // để tránh bị ảnh hưởng nếu người dùng vừa test Postman trước khi chạy bộ test
        entityManager.createNativeQuery("DELETE FROM gio_hang WHERE nguoi_dung_id = 'nd_kh01'").executeUpdate();
        entityManager.createNativeQuery(
                "INSERT INTO gio_hang (nguoi_dung_id, sach_id, so_luong, da_chon) "
                        + "VALUES ('nd_kh01', 'sach_011', 1, TRUE), ('nd_kh01', 'sach_026', 2, TRUE)"
        ).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private String bearer(String userId, String email, VaiTro vaiTro) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, email, vaiTro);
    }

    // --- Nhóm 1: GET /api/v1/vouchers/active (công khai) ---

    @Test
    void layVoucherDangMo_khongCanToken_traVe5MaConHanVaDungDanhMuc() throws Exception {
        mockMvc.perform(get(BASE + "/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[*].maCode", hasItem("CONGNGHE25")))
                .andExpect(jsonPath("$.data[*].maCode", not(hasItem("HETHAN10"))))
                .andExpect(jsonPath("$.data[?(@.maCode=='CONGNGHE25')].danhMucId", hasItem("dm_cong_nghe")));
    }

    // --- Nhóm 2: POST /api/v1/vouchers/validate thành công (theo giỏ thật & tamTinh thủ công) ---

    @Test
    void apMaToanShopBangGioHangThat_khongPhanBietHoaThuongVaGioiHanGiamToiDa() throws Exception {
        // Giỏ mặc định nd_kh01: sach_011 (330k) + sach_026 (2*248k) = 826.000đ (>= 300k freeship)
        // SACHHAY15 giảm 15% của 826k = 123.900đ -> bị trần giamToiDa = 50.000đ
        mockMvc.perform(post(BASE + "/validate")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"sachhay15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voucher.maCode").value("SACHHAY15"))
                .andExpect(jsonPath("$.data.tamTinh").value(826000))
                .andExpect(jsonPath("$.data.tamTinhApDungVoucher").value(826000))
                .andExpect(jsonPath("$.data.phiVanChuyen").value(0))
                .andExpect(jsonPath("$.data.tienGiamGia").value(50000))
                .andExpect(jsonPath("$.data.tongThanhToanDuKien").value(776000));
    }

    @Test
    void apMaBangTamTinhThuCong_tinhDungShipVaGiamCoDinh() throws Exception {
        // Tam tinh 200.000đ (< 300k nên ship 25k), CHAOBAN20 giảm 20.000đ -> tổng 205.000đ
        mockMvc.perform(post(BASE + "/validate")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"CHAOBAN20\",\"tamTinh\":200000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tamTinh").value(200000))
                .andExpect(jsonPath("$.data.phiVanChuyen").value(25000))
                .andExpect(jsonPath("$.data.tienGiamGia").value(20000))
                .andExpect(jsonPath("$.data.tongThanhToanDuKien").value(205000));
    }

    @Test
    void apMaCongNghe25TuGioHang_chiTinhTienSachCongNgheVaDanhMucCon() throws Exception {
        // Ban đầu giỏ nd_kh01 chỉ có Văn học (sach_011) + Trinh thám (sach_026) -> áp CONGNGHE25 phải báo 400
        mockMvc.perform(post(BASE + "/validate")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"CONGNGHE25\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mã CONGNGHE25 chỉ áp dụng cho sách thuộc danh mục 'Công nghệ & Lập trình'."));

        // Thêm 1 cuốn dm_cong_nghe là sach_113 (255.000đ < 350.000đ) -> báo còn thiếu 95.000đ tiền sách Công nghệ
        mockMvc.perform(post("/api/v1/cart")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sachId\":\"sach_113\",\"soLuong\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/validate")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"CONGNGHE25\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("còn thiếu 95.000đ")));

        // Thêm tiếp 1 cuốn danh mục con dm_kien_truc_pm là sach_129 (144.000đ)
        // Tổng riêng nhóm Công nghệ = 255.000 + 144.000 = 399.000đ (>= 350k), còn toàn giỏ = 826k + 399k = 1.225.000đ
        mockMvc.perform(post("/api/v1/cart")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sachId\":\"sach_129\",\"soLuong\":1}"))
                .andExpect(status().isOk());

        // 25% của 399.000 = 99.750đ -> bị trần giamToiDa = 80.000đ -> tổng thanh toán = 1.225.000 - 80.000 = 1.145.000đ
        mockMvc.perform(post(BASE + "/validate")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"CONGNGHE25\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voucher.danhMucId").value("dm_cong_nghe"))
                .andExpect(jsonPath("$.data.tamTinh").value(1225000))
                .andExpect(jsonPath("$.data.tamTinhApDungVoucher").value(399000))
                .andExpect(jsonPath("$.data.tienGiamGia").value(80000))
                .andExpect(jsonPath("$.data.tongThanhToanDuKien").value(1145000));
    }

    @Test
    void apMaCongNghe25BangTamTinhThuCongVaDanhMucId_kiemTraDungDanhMucCon() throws Exception {
        // dm_kien_truc_pm là danh mục con của dm_cong_nghe -> hợp lệ, 25% của 360.000 = 90k -> trần 80k
        mockMvc.perform(post(BASE + "/validate")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"CONGNGHE25\",\"tamTinh\":360000,\"danhMucId\":\"dm_kien_truc_pm\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tienGiamGia").value(80000))
                .andExpect(jsonPath("$.data.tongThanhToanDuKien").value(280000));

        // Chọn danh mục khác (dm_van_hoc) -> bị từ chối 400
        mockMvc.perform(post(BASE + "/validate")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"CONGNGHE25\",\"tamTinh\":500000,\"danhMucId\":\"dm_van_hoc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mã CONGNGHE25 chỉ áp dụng cho sách thuộc danh mục 'Công nghệ & Lập trình'."));
    }

    // --- Nhóm 3: Các ca lỗi nghiệp vụ & bảo mật (400 / 404 / 401 / 403) ---

    @Test
    void apMaThieuDonToiThieu_tra400KemSoTienConThieu() throws Exception {
        mockMvc.perform(post(BASE + "/validate")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"VIP100K\",\"tamTinh\":500000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Đơn hàng cần tối thiểu 700.000đ để sử dụng mã VIP100K (còn thiếu 200.000đ)."));
    }

    @Test
    void apMaDaHetHanHoacTamNgung_tra400() throws Exception {
        mockMvc.perform(post(BASE + "/validate")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"HETHAN10\",\"tamTinh\":500000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mã giảm giá HETHAN10 hiện đang tạm ngưng."));
    }

    @Test
    void apMaKhongTonTai_tra404() throws Exception {
        mockMvc.perform(post(BASE + "/validate")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"KHONGCO\",\"tamTinh\":500000}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy mã giảm giá KHONGCO."));
    }

    @Test
    void baoMatVaPhanQuyenValidateVoucher_tra401Va403() throws Exception {
        mockMvc.perform(post(BASE + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"SACHHAY15\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(BASE + "/validate")
                        .header("Authorization", bearer("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"SACHHAY15\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE + "/validate")
                        .header("Authorization", bearer("nd_kh09", "kiet.do@gmail.com", VaiTro.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maCode\":\"SACHHAY15\"}"))
                .andExpect(status().isForbidden());
    }
}
