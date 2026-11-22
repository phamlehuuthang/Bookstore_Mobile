package com.trithucviet.bookstore;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Kiểm thử tích hợp toàn bộ API Xác thực & Hồ sơ (Bước 2 - phần 1) trên dữ liệu thật bookstore_db.
 * - @Transactional ở cấp class tự động rollback mọi bản ghi đăng ký/cập nhật/đổi mật khẩu sau mỗi test.
 * - Đánh dấu chứng minh JWT sinh ra từ API Login/Register dùng được ngay cho Giỏ hàng (/api/v1/cart)
 *   và Quản trị (/api/admin/vouchers).
 */
@SpringBootTest
@Transactional
class AuthApiTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // --- Nhóm 1: Đăng ký tài khoản Mobile (/api/v1/auth/register) ---

    @Test
    void dangKyMoi_traVeTokenVaDungDuocChoMeVaGioHang() throws Exception {
        String registerBody = """
                {
                  "hoTen": "Nguyễn Minh Khoa",
                  "email": "Khoa.Test99@gmail.com",
                  "soDienThoai": "0988777666",
                  "matKhau": "matkhau123"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("khoa.test99@gmail.com"))
                .andExpect(jsonPath("$.data.user.vaiTro").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.user.biKhoa").value(false))
                .andExpect(jsonPath("$.data.user.matKhauMaHoa").doesNotExist())
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");

        // Dùng chính JWT mới đăng ký để gọi /me và /api/v1/cart
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hoTen").value("Nguyễn Minh Khoa"));

        mockMvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tongSoLuongTrongGio").value(0));

        // Đăng nhập lại bằng mật khẩu vừa đăng ký (BCrypt)
        String loginBody = """
                {
                  "email": "khoa.test99@gmail.com",
                  "matKhau": "matkhau123"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("khoa.test99@gmail.com"));
    }

    @Test
    void dangKy_trungEmailHoacDuLieuKhongHopLe_biChan400() throws Exception {
        // Trùng email khách mẫu nd_kh01 (khác hoa thường vẫn phải chặn)
        String duplicateBody = """
                {
                  "hoTen": "Người Trùng",
                  "email": "THANG.NGUYEN@GMAIL.COM",
                  "soDienThoai": "0912345601",
                  "matKhau": "123456"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Email sai định dạng + mật khẩu < 6 ký tự
        String invalidBody = """
                {
                  "hoTen": "",
                  "email": "khong-phai-email",
                  "matKhau": "123"
                }
                """;
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.hoTen").isNotEmpty())
                .andExpect(jsonPath("$.data.email").isNotEmpty())
                .andExpect(jsonPath("$.data.matKhau").isNotEmpty());
    }

    // --- Nhóm 2: Đăng nhập Mobile App (/api/v1/auth/login) & Platform Guard ---

    @Test
    void dangNhapMobile_khachMauThanhCongVaGoiDuocGioHang() throws Exception {
        String body = """
                {
                  "email": "thang.nguyen@gmail.com",
                  "matKhau": "123456"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value("nd_kh01"))
                .andExpect(jsonPath("$.data.user.vaiTro").value("CUSTOMER"))
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");

        mockMvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void dangNhapMobile_chanNhanVienAdmin_chanTaiKhoanBiKhoa_saiMatKhau() throws Exception {
        // Admin cố đăng nhập vào app khách hàng -> 403 Platform Guard
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "admin@trithucviet.vn", "matKhau": "123456" }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Tài khoản này không hỗ trợ đăng nhập trên ứng dụng mua sắm. Vui lòng sử dụng tài khoản khách hàng hoặc đăng ký tài khoản mới."));

        // Khách nd_kh09 đang bị khóa -> 403
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "kiet.do@gmail.com", "matKhau": "123456" }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Tài khoản của bạn đang tạm khóa do vi phạm chính sách hoặc lý do bảo mật. Vui lòng liên hệ CSKH để được hỗ trợ."));

        // Sai mật khẩu -> 401
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "thang.nguyen@gmail.com", "matKhau": "sai-mat-khau" }
                                """))
                .andExpect(status().isUnauthorized());

        // Email không tồn tại -> 401
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "khongtontai@gmail.com", "matKhau": "123456" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // --- Nhóm 3: Đăng nhập Web Admin (/api/admin/auth/login) ---

    @Test
    void dangNhapWebAdmin_choPhep3VaiTroNoiBo_chanKhachHang() throws Exception {
        // ADMIN
        MvcResult adminRes = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "admin@trithucviet.vn", "matKhau": "123456" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.vaiTro").value("ADMIN"))
                .andReturn();

        String adminToken = JsonPath.read(adminRes.getResponse().getContentAsString(), "$.data.accessToken");

        // Dùng token đăng nhập Web Admin gọi API quản lý Voucher -> 200 OK
        mockMvc.perform(get("/api/admin/vouchers").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // CSKH
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "cskh@trithucviet.vn", "matKhau": "123456" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.vaiTro").value("CSKH"));

        // WAREHOUSE_MANAGER
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "kho@trithucviet.vn", "matKhau": "123456" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.vaiTro").value("WAREHOUSE_MANAGER"));

        // CUSTOMER cố đăng nhập Web Admin -> 403 Forbidden
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "thang.nguyen@gmail.com", "matKhau": "123456" }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Tài khoản này không có quyền truy cập Cổng quản trị nội bộ."));
    }

    // --- Nhóm 4: Hồ sơ cá nhân & Đổi mật khẩu (/me, /profile, /password) ---

    @Test
    void xemVaCapNhatHoSo_baoVeKhiThieuTokenVaTaiKhoanBiKhoa() throws Exception {
        // Không gửi token -> phải trả 401 ở cả 3 API bảo vệ
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "hoTen": "Test", "soDienThoai": "0912345678" }
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "matKhauCu": "123456", "matKhauMoi": "654321" }
                                """))
                .andExpect(status().isUnauthorized());

        // Tài khoản bị khóa cầm JWT cũ gọi /me -> bị chặn 403
        String lockedToken = "Bearer " + jwtTokenProvider.generateToken("nd_kh09", "kiet.do@gmail.com", VaiTro.CUSTOMER);
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", lockedToken))
                .andExpect(status().isForbidden());

        // Khách nd_kh01 cập nhật hồ sơ thành công
        String customerToken = "Bearer " + jwtTokenProvider.generateToken("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);
        mockMvc.perform(put("/api/v1/auth/profile")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "hoTen": "Nguyễn Hữu Thắng (VIP)", "soDienThoai": "0999888777" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hoTen").value("Nguyễn Hữu Thắng (VIP)"))
                .andExpect(jsonPath("$.data.soDienThoai").value("0999888777"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hoTen").value("Nguyễn Hữu Thắng (VIP)"));
    }

    @Test
    void doiMatKhau_thanhCongVaKhoaMatKhauCu() throws Exception {
        String customerToken = "Bearer " + jwtTokenProvider.generateToken("nd_kh01", "thang.nguyen@gmail.com", VaiTro.CUSTOMER);

        // Nhập sai mật khẩu cũ -> 400
        mockMvc.perform(put("/api/v1/auth/password")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "matKhauCu": "khongdung", "matKhauMoi": "matkhauMoi888" }
                                """))
                .andExpect(status().isBadRequest());

        // Mật khẩu mới trùng mật khẩu cũ -> 400
        mockMvc.perform(put("/api/v1/auth/password")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "matKhauCu": "123456", "matKhauMoi": "123456" }
                                """))
                .andExpect(status().isBadRequest());

        // Đổi mật khẩu thành công -> 200
        mockMvc.perform(put("/api/v1/auth/password")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "matKhauCu": "123456", "matKhauMoi": "matkhauMoi888" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công"));

        // Đăng nhập lại bằng mật khẩu cũ "123456" phải bị từ chối 401 (đã chuyển sang BCrypt mới)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "thang.nguyen@gmail.com", "matKhau": "123456" }
                                """))
                .andExpect(status().isUnauthorized());

        // Đăng nhập bằng mật khẩu mới thành công 200
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "thang.nguyen@gmail.com", "matKhau": "matkhauMoi888" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value("nd_kh01"));
    }
}
