package com.trithucviet.bookstore;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Kiểm thử tích hợp API Quản lý Khách hàng & Phân quyền Nhân sự Web Admin (Bước 2 - Phần 3) trên bookstore_db.
 * - @Transactional tự rollback mọi thay đổi trong test nên không làm bẩn DB mẫu.
 * - Kiểm tra đầy đủ 4 vai trò: ADMIN (toàn quyền), CSKH (chỉ xem khách hàng), WAREHOUSE_MANAGER (403), CUSTOMER (403).
 */
@SpringBootTest
@Transactional
class AdminUserApiTests {

    private static final String BASE = "/api/admin/users";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private String adminToken;
    private String cskhToken;
    private String warehouseToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        adminToken = bearer("nd_admin", "admin@trithucviet.vn", VaiTro.ADMIN);
        cskhToken = bearer("nd_cskh", "cskh@trithucviet.vn", VaiTro.CSKH);
        warehouseToken = bearer("nd_kho", "kho@trithucviet.vn", VaiTro.WAREHOUSE_MANAGER);
        customerToken = bearer("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);
    }

    private String bearer(String userId, String email, VaiTro vaiTro) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, email, vaiTro);
    }

    @Test
    void adminVaCskh_xemDanhSachKhachHang_thanhCongVaHoTroTimKiem() throws Exception {
        mockMvc.perform(get(BASE + "/customers").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(9)))
                .andExpect(jsonPath("$.data[*].email", hasItem("thang.nguyen@gmail.com")));

        mockMvc.perform(get(BASE + "/customers")
                        .param("keyword", "thang.nguyen")
                        .header("Authorization", cskhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value("nd_kh01"));
    }

    @Test
    void phanQuyenRbac_chanCskhKhoVaCustomerVaoApiNhanSuVaKhoaTaiKhoan() throws Exception {
        // CSKH không được xem danh sách nhân sự, tạo nhân sự, khóa tài khoản hoặc đổi quyền
        mockMvc.perform(get(BASE + "/staff").header("Authorization", cskhToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(BASE + "/nd_kh01/lock").header("Authorization", cskhToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(BASE + "/nd_kho/role")
                        .header("Authorization", cskhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vaiTro\":\"CSKH\"}"))
                .andExpect(status().isForbidden());

        // WAREHOUSE_MANAGER & CUSTOMER bị chặn hoàn toàn
        mockMvc.perform(get(BASE + "/customers").header("Authorization", warehouseToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/customers").header("Authorization", customerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/staff")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hoTen\":\"A\",\"email\":\"a@trithucviet.vn\",\"soDienThoai\":\"0909000111\",\"vaiTro\":\"CSKH\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_taoNhanSuMoi_khoaMoTaiKhoan_vaDoiVaiTro_thanhCong() throws Exception {
        String createPayload = """
                {
                  "hoTen": "Nguyễn Hoàng Minh",
                  "email": "minh.staff@trithucviet.vn",
                  "soDienThoai": "0909888777",
                  "vaiTro": "WAREHOUSE_MANAGER",
                  "matKhau": "123456"
                }
                """;

        MvcResult created = mockMvc.perform(post(BASE + "/staff")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("minh.staff@trithucviet.vn"))
                .andExpect(jsonPath("$.data.vaiTro").value("WAREHOUSE_MANAGER"))
                .andExpect(jsonPath("$.data.biKhoa").value(false))
                .andReturn();

        String newStaffId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        // Danh sách nhân sự phải chứa nhân viên vừa tạo
        mockMvc.perform(get(BASE + "/staff")
                        .param("keyword", "minh.staff")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(newStaffId));

        // Trùng email phải báo 400
        mockMvc.perform(post(BASE + "/staff")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isBadRequest());

        // Đổi vai trò nhân viên vừa tạo sang CSKH
        mockMvc.perform(patch(BASE + "/" + newStaffId + "/role")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vaiTro\":\"CSKH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vaiTro").value("CSKH"));

        // Khóa và mở khóa lại tài khoản khách hàng nd_kh01
        mockMvc.perform(patch(BASE + "/nd_kh01/lock").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.biKhoa").value(true));

        mockMvc.perform(patch(BASE + "/nd_kh01/lock").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.biKhoa").value(false));
    }

    @Test
    void admin_khongTheTuKhoaHoacTuDoiQuyenChinhMinh_vaChanCustomerRole() throws Exception {
        // Admin tự khóa chính mình -> 400
        mockMvc.perform(patch(BASE + "/nd_admin/lock").header("Authorization", adminToken))
                .andExpect(status().isBadRequest());

        // Admin tự đổi vai trò chính mình -> 400
        mockMvc.perform(patch(BASE + "/nd_admin/role")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vaiTro\":\"CSKH\"}"))
                .andExpect(status().isBadRequest());

        // Không cho cấp tài khoản nhân sự với vai trò CUSTOMER -> 400
        mockMvc.perform(post(BASE + "/staff")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hoTen\":\"Khach\",\"email\":\"khach.nv@trithucviet.vn\",\"soDienThoai\":\"0909111222\",\"vaiTro\":\"CUSTOMER\"}"))
                .andExpect(status().isBadRequest());

        // Không cho đổi vai trò của tài khoản CUSTOMER -> 400
        mockMvc.perform(patch(BASE + "/nd_kh01/role")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vaiTro\":\"CSKH\"}"))
                .andExpect(status().isBadRequest());
    }
}
