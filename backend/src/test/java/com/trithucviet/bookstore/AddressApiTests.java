package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.trithucviet.bookstore.config.JwtTokenProvider;
import com.trithucviet.bookstore.entity.enums.VaiTro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Kiểm thử tích hợp toàn bộ API Sổ địa chỉ nhận hàng (Bước 2 - phần 2) trên bookstore_db.
 * - @Transactional ở cấp class tự rollback mọi thay đổi trên so_dia_chi sau mỗi test.
 * - Kiểm tra dữ liệu mẫu nd_kh01 (đang có dc_01 mặc định và dc_02 không mặc định) và dc_03 (của nd_kh02).
 */
@SpringBootTest
@Transactional
class AddressApiTests {

    private static final String BASE = "/api/v1/addresses";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private String customerToken;
    private String lockedCustomerToken;
    private String adminToken;
    private String cskhToken;
    private String warehouseToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        customerToken = bearer("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);
        lockedCustomerToken = bearer("nd_kh09", "kiet.do@gmail.com", VaiTro.CUSTOMER);
        adminToken = bearer("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN);
        cskhToken = bearer("nd_cskh", "cskh@trithucviet.vn", VaiTro.CSKH);
        warehouseToken = bearer("nd_kho", "kho@trithucviet.vn", VaiTro.WAREHOUSE_MANAGER);
    }

    private String bearer(String userId, String email, VaiTro vaiTro) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, email, vaiTro);
    }

    // --- Nhóm 1: Xem danh sách & Tạo mới địa chỉ ---

    @Test
    void laySoDiaChi_traVeMacDinhDauTienVaDiaChiDayDu() throws Exception {
        mockMvc.perform(get(BASE).header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value("dc_01"))
                .andExpect(jsonPath("$.data[0].macDinh").value(true))
                .andExpect(jsonPath("$.data[0].diaChiDayDu")
                        .value("12 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh"))
                .andExpect(jsonPath("$.data[1].id").value("dc_02"))
                .andExpect(jsonPath("$.data[1].macDinh").value(false));
    }

    @Test
    void themDiaChiMoiMacDinh_tuDongHaMacDinhCu() throws Exception {
        String body = """
                {
                  "hoTenNguoiNhan": "Nguyễn Hữu Thắng (Nhà mới)",
                  "soDienThoai": "0909888777",
                  "tinhThanh": "TP. Hồ Chí Minh",
                  "quanHuyen": "Quận 7",
                  "phuongXa": "Phường Tân Phong",
                  "diaChiChiTiet": "19 Nguyễn Hữu Thọ",
                  "macDinh": true
                }
                """;

        MvcResult created = mockMvc.perform(post(BASE)
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.macDinh").value(true))
                .andExpect(jsonPath("$.data.diaChiDayDu")
                        .value("19 Nguyễn Hữu Thọ, Phường Tân Phong, Quận 7, TP. Hồ Chí Minh"))
                .andReturn();

        String newId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        // Kiểm tra lại danh sách: tổng 3 địa chỉ, chỉ địa chỉ mới là macDinh = true, dc_01 & dc_02 đều false
        mockMvc.perform(get(BASE).header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].id").value(newId))
                .andExpect(jsonPath("$.data[0].macDinh").value(true))
                .andExpect(jsonPath("$.data[1].id").value("dc_01"))
                .andExpect(jsonPath("$.data[1].macDinh").value(false))
                .andExpect(jsonPath("$.data[2].id").value("dc_02"))
                .andExpect(jsonPath("$.data[2].macDinh").value(false));
    }

    @Test
    void khachChuaCoDiaChi_themDiaChiDauTienDuGuiFalseVanTuDongThanhMacDinh() throws Exception {
        // Tạo khách hàng mới hoàn toàn chưa có địa chỉ qua API đăng ký
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hoTen": "Khách Mới Địa Chỉ",
                                  "email": "newaddr.test@gmail.com",
                                  "soDienThoai": "0911223344",
                                  "matKhau": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String newUserBearer = "Bearer " + JsonPath.read(regResult.getResponse().getContentAsString(), "$.data.accessToken");

        // Gửi macDinh = false cho địa chỉ đầu tiên -> Quy tắc 2A phải tự nâng lên true
        mockMvc.perform(post(BASE)
                        .header("Authorization", newUserBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hoTenNguoiNhan": "Khách Mới Địa Chỉ",
                                  "soDienThoai": "0911223344",
                                  "tinhThanh": "Hà Nội",
                                  "quanHuyen": "Quận Hai Bà Trưng",
                                  "phuongXa": "Phường Bách Khoa",
                                  "diaChiChiTiet": "Số 1 Đại Cồ Việt",
                                  "macDinh": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.macDinh").value(true));
    }

    // --- Nhóm 2: Cập nhật, Đặt mặc định & Validation ---

    @Test
    void capNhatVaDatMacDinh_huyMacDinhCuDungQuyTac() throws Exception {
        // Cập nhật dc_02 giữ nguyên macDinh (không truyền) -> vẫn false
        mockMvc.perform(put(BASE + "/dc_02")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hoTenNguoiNhan": "Nguyễn Hữu Thắng (Văn phòng mới)",
                                  "soDienThoai": "0912345699",
                                  "tinhThanh": "TP. Hồ Chí Minh",
                                  "quanHuyen": "Quận 3",
                                  "phuongXa": "Phường Võ Thị Sáu",
                                  "diaChiChiTiet": "99 Pasteur"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hoTenNguoiNhan").value("Nguyễn Hữu Thắng (Văn phòng mới)"))
                .andExpect(jsonPath("$.data.macDinh").value(false));

        // Bấm đặt dc_02 làm mặc định qua PATCH /{id}/default
        mockMvc.perform(patch(BASE + "/dc_02/default").header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("dc_02"))
                .andExpect(jsonPath("$.data.macDinh").value(true));

        // Kiểm tra lại danh sách: dc_02 lên vị trí đầu, dc_01 xuống false
        mockMvc.perform(get(BASE).header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("dc_02"))
                .andExpect(jsonPath("$.data[0].macDinh").value(true))
                .andExpect(jsonPath("$.data[1].id").value("dc_01"))
                .andExpect(jsonPath("$.data[1].macDinh").value(false));
    }

    @Test
    void themHoacSuaDiaChi_duLieuKhongHopLe_biChan400() throws Exception {
        mockMvc.perform(post(BASE)
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hoTenNguoiNhan": "",
                                  "soDienThoai": "abc",
                                  "tinhThanh": "",
                                  "quanHuyen": "Quận 1",
                                  "phuongXa": "Phường 1",
                                  "diaChiChiTiet": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.hoTenNguoiNhan").isNotEmpty())
                .andExpect(jsonPath("$.data.soDienThoai").isNotEmpty())
                .andExpect(jsonPath("$.data.tinhThanh").isNotEmpty())
                .andExpect(jsonPath("$.data.diaChiChiTiet").isNotEmpty());
    }

    // --- Nhóm 3: Xóa địa chỉ mặc định tự đôn địa chỉ còn lại (Quy tắc 1A) ---

    @Test
    void xoaDiaChiMacDinh_tuDonDiaChiConLaiLenMacDinhVaXoaHet() throws Exception {
        // Xóa dc_01 (đang là mặc định) -> dc_02 tự động được đôn lên macDinh = true
        mockMvc.perform(delete(BASE + "/dc_01").header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value("dc_02"))
                .andExpect(jsonPath("$.data[0].macDinh").value(true));

        // Xóa tiếp dc_02 (địa chỉ cuối cùng) -> trả về mảng rỗng []
        mockMvc.perform(delete(BASE + "/dc_02").header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // --- Nhóm 4: Bảo mật chống IDOR, khóa tài khoản & phân quyền ---

    @Test
    void chongIdor_chanTaiKhoanKhoa_chanNhanVienVaThieuToken() throws Exception {
        String validBody = """
                {
                  "hoTenNguoiNhan": "Hacker",
                  "soDienThoai": "0901234567",
                  "tinhThanh": "Hà Nội",
                  "quanHuyen": "Quận Cầu Giấy",
                  "phuongXa": "Phường Dịch Vọng",
                  "diaChiChiTiet": "45 Xuân Thủy"
                }
                """;

        // nd_kh01 cố sửa / đặt mặc định / xóa địa chỉ dc_03 của nd_kh02 -> 404 Not Found
        mockMvc.perform(put(BASE + "/dc_03")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch(BASE + "/dc_03/default").header("Authorization", customerToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE + "/dc_03").header("Authorization", customerToken))
                .andExpect(status().isNotFound());

        // Không gửi token -> 401 Unauthorized
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        // Khách bị khóa nd_kh09 -> 403 Forbidden
        mockMvc.perform(get(BASE).header("Authorization", lockedCustomerToken))
                .andExpect(status().isForbidden());

        // Nhân sự ADMIN / CSKH / WAREHOUSE_MANAGER -> 403 Forbidden
        mockMvc.perform(get(BASE).header("Authorization", adminToken)).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE).header("Authorization", cskhToken)).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE).header("Authorization", warehouseToken)).andExpect(status().isForbidden());
    }
}
